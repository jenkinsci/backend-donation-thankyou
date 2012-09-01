package org.jenkinsci.backend.donation;

import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.ServiceException;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

        FileInputStream in = new FileInputStream(email);
        try {
            MimeMessage msg = new MimeMessage(Session.getDefaultInstance(System.getProperties()), in);
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
        } finally {
            in.close();
        }
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
    public void insert(WorksheetEntry worksheet) throws IOException, ServiceException {
        // Create a local representation of the new row.
        ListEntry row = new ListEntry();
        row.getCustomElements().setValueLocal("amount", amount);
        row.getCustomElements().setValueLocal("name", name);
        row.getCustomElements().setValueLocal("e-mail", email);
        row.getCustomElements().setValueLocal("date", new SimpleDateFormat("MM/dd/yyyy").format(date));
        row.getCustomElements().setValueLocal("friend", isFriend() ? "Yes" : "-");

        // insert this row
        worksheet.getService().insert(worksheet.getListFeedUrl(), row);
    }

    @Override
    public int compareTo(Donation that) {
        return this.date.compareTo(that.date);
    }

    private static final Pattern SUBJECT = Pattern.compile("Receipt \\[\\$([0-9.]+)\\] By: (.+) \\[(.+)\\]");
}
