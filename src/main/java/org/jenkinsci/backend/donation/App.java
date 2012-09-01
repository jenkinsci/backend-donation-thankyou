package org.jenkinsci.backend.donation;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class App
{
    SpreadsheetService service = new SpreadsheetService("Jenkins donation tracking");

    @Argument(required=true,metaVar="DIR")
    File dir;

    public static void main(String[] args) throws Exception {
        App app = new App();
        new CmdLineParser(app).parseArgument(args);
        app.run();
    }

    public void run() throws Exception {
        login();
        WorksheetEntry worksheet = findSpreadsheet();

//        // Iterate through each row, printing its cell values.
//        ListFeed listFeed = service.getFeed(listFeedUrl, ListFeed.class);
//        for (ListEntry row : listFeed.getEntries()) {
//            // Iterate over the remaining columns, and print each cell value
//            for (String tag : row.getCustomElements().getTags()) {
//                System.out.print(tag+'='+row.getCustomElements().getValue(tag) + "\t");
//            }
//            System.out.println();
//        }

        List<Donation> donations = new ArrayList<Donation>();
        File[] files = dir.listFiles();
        for (File file : files) {
            Donation d = Donation.parse(file);
            if (d==null)
                System.err.println("Ignoring "+file);
            else
                donations.add(d);
        }

        Collections.sort(donations);

        for (Donation d : donations) {
            System.out.println(d.name);
            d.insert(worksheet);
        }
    }

    /**
     * Finds the right worksheet to touch
     */
    private WorksheetEntry findSpreadsheet() throws IOException, ServiceException {
        // Make a request to the API and get all spreadsheets.
        SpreadsheetEntry book = service.getEntry(new URL("https://spreadsheets.google.com/feeds/spreadsheets/tS4k0cWPEDy1qPSKT5r8sWw"),SpreadsheetEntry.class);
//        System.out.println(book.getTitle().getPlainText());
//        System.out.println();

        WorksheetFeed sheet = service.getFeed(book.getWorksheetFeedUrl(), WorksheetFeed.class);
        List<WorksheetEntry> worksheets = sheet.getEntries();
        return worksheets.get(0);
    }

    private void login() throws IOException, AuthenticationException {
        Properties props = new Properties();
        props.load(new FileInputStream(new File(new File(System.getProperty("user.home")), ".google")));

        service.setUserCredentials(props.getProperty("userName"), props.getProperty("password"));
        service.setProtocolVersion(SpreadsheetService.Versions.V3);
    }

    private static void playWithCells(WorksheetEntry worksheet) throws IOException, ServiceException {
        // Fetch the cell feed of the worksheet.
        URL cellFeedUrl = worksheet.getCellFeedUrl();
        CellFeed cellFeed = worksheet.getService().getFeed(cellFeedUrl, CellFeed.class);

        for (CellEntry cell : cellFeed.getEntries()) {
            // Print the cell's address in A1 notation
            System.out.print(cell.getTitle().getPlainText() + "\t");
            // Print the cell's formula or text value
            System.out.print(cell.getCell().getInputValue() + "\t");
            System.out.println();
        }
    }

    private static void listSpreadSheets(SpreadsheetService service) throws IOException, ServiceException {
        URL SPREADSHEET_FEED_URL = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full");
        SpreadsheetFeed feed = service.getFeed(SPREADSHEET_FEED_URL,SpreadsheetFeed.class);
        List<SpreadsheetEntry> spreadsheets = feed.getEntries();

        for (SpreadsheetEntry sheet : spreadsheets) {
            System.out.println(sheet.getId());
            System.out.println(sheet.getTitle().getPlainText());
        }
    }
}
