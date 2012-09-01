package org.jenkinsci.backend.donation;

import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.ServiceException;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kohsuke Kawaguchi
 */
public class Donation implements Comparable<Donation> {
    /**
     * Dollar figure of the donation in String format, like "10.00"
     */
    public String amount;
    /**
     * Name of the person donated, and his e-mail address.
     */
    public String name,email;

    /**
     * When the donation happened?
     */
    public Date date;

    public static Donation parse(File email) throws Exception {
        if (!email.getName().endsWith(".eml"))    return null;

        MimeMessage msg = read(new FileInputStream(email));
        Matcher m = SUBJECT.matcher(msg.getSubject());
        if (!m.matches()) {
            return null;
        }

        Donation d = new Donation();
        d.amount = m.group(1);
        d.name = m.group(2);
        d.email = m.group(3);
        d.date = new Date(msg.getHeader("Date")[0]);

        return d;
    }

    /**
     * Donate $25 or more and you get honoraly "Friend of Jenkins" title.
     */
    public boolean isFriend() {
        return Float.parseFloat(amount) >= 25;
    }

    /**
     * Inserts this record into the spreadsheet.
     */
    public void insert(WorksheetEntry worksheet) throws IOException, ServiceException, MessagingException {
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");

        // Create a local representation of the new row.
        ListEntry row = new ListEntry();
        row.getCustomElements().setValueLocal("amount", amount);
        row.getCustomElements().setValueLocal("name", name);
        row.getCustomElements().setValueLocal("e-mail", email);
        row.getCustomElements().setValueLocal("date", df.format(date));
        row.getCustomElements().setValueLocal("e-mailsent", df.format(new Date()));
        row.getCustomElements().setValueLocal("friend", isFriend() ? "Yes" : "-");

        // insert this row
        worksheet.getService().insert(worksheet.getListFeedUrl(), row);

        // send e-mail
        Message msg = read(getClass().getResourceAsStream(isFriend() ? "/friend.eml" : "/thankyou.eml"));
        msg.setRecipient(RecipientType.TO, new InternetAddress(email,name));
        msg.setHeader("Date",new Date().toGMTString());
        Transport.send(msg);
    }

    @Override
    public int compareTo(Donation that) {
        return this.date.compareTo(that.date);
    }

    private static MimeMessage read(InputStream in) throws MessagingException,IOException {
        try {
            return new MimeMessage(Session.getDefaultInstance(System.getProperties()), in);
        } finally {
            in.close();
        }
    }

    /**
     * Expected subject line.
     */
    private static final Pattern SUBJECT = Pattern.compile("Receipt \\[\\$([0-9.]+)\\] By: (.+) \\[(.+)\\]");
}
