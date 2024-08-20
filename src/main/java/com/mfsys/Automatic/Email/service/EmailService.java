package com.mfsys.Automatic.Email.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfsys.Automatic.Email.model.FetchOrderTable;
import com.mfsys.Automatic.Email.repository.OrderRepository;
//import tech.dragon.tabula.PDFTabulaExtractor;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.thymeleaf.context.Context;
import com.itextpdf.html2pdf.HtmlConverter;
import technology.tabula.*;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

@Service
public class EmailService {
    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private OrderRepository orderRepository;

    public void viewEmail(){
        String host = "pop3.mailtrap.io";
        final String user = "abc";
        final String password = "xyz";
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "pop3");
        properties.put("mail.pop3.host", host);
        properties.put("mail.pop3.port", "9950");
        properties.put("mail.pop3.starttls.enable", "true");
        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user,password);
                    }
                });

        try {
            Store store = session.getStore("pop3");
            store.connect(host, user, password);
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    System.in));

            Message[] messages = emailFolder.getMessages();
            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];
                System.out.println("---------------------------------");
                writePart(message);
                String line = reader.readLine();
                System.out.println("type YES if you want to continue, type QUIT if you want to quit");
                if ("yes".equals(line)) {
                    message.writeTo(System.out);
                } else if ("quit".equals(line)) {
                    break;
                }

            }
            System.out.println("messages.length---" + messages.length);

            // close the store and folder objects
            emailFolder.close(false);
            store.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public static void writePart(Part p) throws Exception {
        if (p instanceof Message)
            //Call methos writeEnvelope
            writeEnvelope((Message) p);

        System.out.println("----------------------------");
        System.out.println("CONTENT-TYPE: " + p.getContentType());

        //check if the content is plain text
        if (p.isMimeType("text/plain")) {
            System.out.println("This is plain text");
            System.out.println("---------------------------");
            System.out.println((String) p.getContent());
        }
        //check if the content has attachment
        else if (p.isMimeType("multipart/*")) {
            System.out.println("This is a Multipart");
            System.out.println("---------------------------");
            Multipart mp = (Multipart) p.getContent();
            int count = mp.getCount();
            for (int i = 0; i < count; i++)
                writePart(mp.getBodyPart(i));
        }

        else {
            Object o = p.getContent();
            if (o instanceof InputStream) {
                System.out.println("This is just an input stream");
                System.out.println("---------------------------");
                InputStream is = (InputStream) o;
                is = (InputStream) o;
                int c;
                    try (OutputStream os = new FileOutputStream("output125.pdf");) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;

                        // Read from InputStream and write to OutputStream
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }

                        System.out.println("ended");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

            }
            else {
                System.out.println("This is an unknown type");
                System.out.println("---------------------------");
                System.out.println(o.toString());
            }
        }

    }
    /*
     * This method would print FROM,TO and SUBJECT of the message
     */
    public static void writeEnvelope(Message m) throws Exception {
        System.out.println("This is the message envelope");
        System.out.println("---------------------------");
        Address[] a;

        // FROM
        if ((a = m.getFrom()) != null) {
            for (int j = 0; j < a.length; j++)
                System.out.println("FROM: " + a[j].toString());
        }

        // TO
        if ((a = m.getRecipients(Message.RecipientType.TO)) != null) {
            for (int j = 0; j < a.length; j++)
                System.out.println("TO: " + a[j].toString());
        }

        // SUBJECT
        if (m.getSubject() != null)
            System.out.println("SUBJECT: " + m.getSubject());

    }

    public void sendEmail(){
        String host = "smtp.mailtrap.io";
        final String user = "abc";
        final String password = "xyz";
        String[] to = {"abcd@xya.com"};
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "2525");
        Session session = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user,password);
                    }
                });
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user, "ali"));

            for (String recipient : to) {
                message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(recipient));
            }

            String response = generatePDF();

//            ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();

//            HtmlConverter.convertToPdf(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), pdfOutputStream);
            byte[] pdfOutputStream  = Base64.getDecoder().decode(response);
            Multipart multipart = new MimeMultipart();
            // Body part for the email text
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            multipart.addBodyPart(attachmentBodyPart);
            DataSource source = new ByteArrayDataSource(pdfOutputStream, "application/pdf");
            attachmentBodyPart.setDataHandler(new DataHandler(source));
            attachmentBodyPart.setFileName("anything.pdf");

            // Set the multipart message to the email message
            multipart.addBodyPart(attachmentBodyPart);
            message.setSubject("test");
            message.setText("This is simple program of sending email using JavaMail API");
            message.setContent(multipart);
            Transport.send(message);
            System.out.println("message sent successfully...");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generatePDF() throws Exception {

//        String langFileName = "en";
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML5");
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        Context context = new Context();
        // Add custom variables
        List<Map<String, Object>> dataList = getDummyValue();
        context.setVariable("records", dataList);
        String processedHtml = templateEngine.process("anything", context);
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();

        HtmlConverter.convertToPdf(new ByteArrayInputStream(processedHtml.getBytes(StandardCharsets.UTF_8)), pdfOutputStream);

        return Base64.getEncoder().encodeToString(pdfOutputStream.toByteArray());

    }


    static String getText(File pdfFile) throws IOException {
        PDDocument doc = Loader.loadPDF(pdfFile);
        return new PDFTextStripper().getText(doc);
    }

    public List<FetchOrderTable> printText(){

        try {
            String input = getText(new File("output125.pdf"));
            File file = new File("output125.pdf");
            InputStream in = this.getClass().getClassLoader().getResourceAsStream("output125.pdf");
            List<FetchOrderTable> dataList = new ArrayList<>();
            boolean reached = false;

            try (PDDocument document = Loader.loadPDF(in.readAllBytes())) {
                SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
                PageIterator pi = new ObjectExtractor(document).extract();
                while (pi.hasNext()) {
                    // iterate over the pages of the document
                    Page page = pi.next();
                    List<Table> table = sea.extract(page);
                    // iterate over the tables of the page
                    for(Table tables: table) {
                        List<List<RectangularTextContainer>> rows = tables.getRows();
                        // iterate over the rows of the table
                        for (List<RectangularTextContainer> cells : rows) {
                            FetchOrderTable dataTable = new FetchOrderTable();
                            int index = 0;
                            // print all column-cells of the row plus linefeed
                            for (RectangularTextContainer content : cells) {
                                // Note: Cell.getText() uses \r to concat text chunks
                                String text = content.getText().replace("\r", " ");
                                System.out.print(text + "|");
                                if (text.equals("Capacity")){
                                    reached = true;
                                    continue;
                                }
                                if(reached){
                                    if(index == 0){
                                        dataTable.setTo(text);
                                    }if(index == 1){
                                        dataTable.setFrom(text);
                                    }
                                    if(index == 2){
                                        dataTable.setName(text);
                                    }
                                    if(index == 3){
                                        dataTable.setDate(text);
                                    }
                                    if(index == 4){
                                        dataTable.setTruckType(text);
                                    }
                                    if(index == 5){
                                        dataTable.setTruckName(text);
                                    }
                                    if(index == 6){
                                        dataTable.setModel(text);
                                    }
                                    if(index == 7){
                                        dataTable.setWeight(text);
                                    }
                                    if(index == 8){
                                        dataTable.setCapacity(text);
                                    }
                                    index ++;

                                }
                            }if (dataTable.getFrom() != null){
                            dataList.add(dataTable);}
                            System.out.println();
                        }
                    }
                }
            }
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(new File("output.json"), dataList);
            System.out.println("Data written to JSON file successfully.");
            return dataList;


            // ----------------FOR FUTURE USE IN CASE NEEDED------------
//            Properties props = new Properties();
//            props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse");
//            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
//            Annotation document = new Annotation(input);
//            pipeline.annotate(document);
//            for (CoreLabel token : document.get(CoreAnnotations.TokensAnnotation.class)) {
//                String word = token.get(CoreAnnotations.TextAnnotation.class);
//                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
//                System.out.println(word + " : " + ne);
//            }
//            SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
//            String[] tokens = tokenizer.tokenize(input);
//            for (String token : tokens) {
//                System.out.println(token);
//            }
//            Map<String, Object> fieldMap = new HashMap<>();
//            Pattern toPattern = Pattern.compile("To:\\s*(.*)");
//            Pattern fromPattern = Pattern.compile("From:\\s*(.*)");
//            Pattern amountPattern = Pattern.compile("Amont:\\s*(\\d+),?");
//
//            // Extract and store the values using regex
//            fieldMap.put("to", extractValue(input, toPattern));
//            fieldMap.put("from", extractValue(input, fromPattern));
//            fieldMap.put("amount", extractValue(input, amountPattern));
//            FreightOrder order = new FreightOrder();
//            order.setAmount((String) fieldMap.get("amount"));
//            order.setDestination((String) fieldMap.get("to"));
//            order.setPickupFrom((String) fieldMap.get("from"));
//            orderRepository.save(order);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Map<String, Object>> getDummyValue(){
        List<Map<String, Object>> records = new ArrayList<>();
        String[][] data = {
                {"Norma Logan", "South Scottland", "Nancyview", "2499.28", "2024-03-12", "P55", "Refrigerated", "2021", "13757KG", "15034KG"},
                {"Ethel Brewer", "North Korybury", "Lake Payton", "3943.59", "2023-11-07", "P40", "Tanker", "2022", "18824KG", "12824KG"},
                {"Rufus Mann", "West Vaughnside", "West Thomas", "2177.43", "2023-09-21", "P60", "Flatbed", "2020", "16590KG", "18040KG"},
                {"Pearl Wallace", "East Stanley", "South Kellyport", "3068.99", "2024-02-25", "P70", "Dry Van", "2019", "14987KG", "14000KG"},
                {"Clayton Morris", "North Jerryshire", "Lake Marissaborough", "4783.24", "2023-07-14", "P45", "Lowboy", "2021", "20015KG", "22000KG"},
                {"Cora Jennings", "West Arnoldview", "East Sherry", "1923.77", "2024-01-10", "P50", "Tanker", "2022", "17584KG", "18530KG"},
                {"Jasper Barnett", "South Joeton", "Port Joshuafurt", "4156.48", "2023-10-28", "P65", "Refrigerated", "2021", "21030KG", "23050KG"},
                {"Madeline Lane", "North Juliefort", "Lake Michaelside", "3538.91", "2024-04-18", "P42", "Flatbed", "2020", "19856KG", "21020KG"},
                {"Nettie Harmon", "East Edwardberg", "New Patmouth", "2839.12", "2023-12-05", "P67", "Lowboy", "2019", "18560KG", "20015KG"},
                {"Florence Anderson", "West Geraldport", "Port Hayleytown", "2294.88", "2023-08-22", "P58", "Dry Van", "2022", "15560KG", "17030KG"},
                {"Dean Johnston", "South Haroldstad", "North Codytown", "4893.29", "2024-06-03", "P61", "Tanker", "2020", "16800KG", "19000KG"},
                {"Jessie Ferguson", "East Glennborough", "Lake Derrickburgh", "2777.34", "2023-11-29", "P54", "Refrigerated", "2021", "17890KG", "19450KG"},
                {"Rachel Caldwell", "West Adrianshire", "Port Calebfort", "3416.67", "2024-03-15", "P47", "Flatbed", "2020", "19999KG", "20900KG"},
                {"Hugh Maxwell", "North Lewisport", "West Arielton", "2347.82", "2023-10-12", "P44", "Lowboy", "2019", "18450KG", "20000KG"},
                {"Violet Burke", "South Patriciafort", "New Nicoleshire", "3719.55", "2024-02-04", "P66", "Dry Van", "2022", "14000KG", "15800KG"},
                {"Oscar Goodwin", "East Kimberleyfort", "Port Gabrielfort", "2145.88", "2023-07-25", "P43", "Tanker", "2021", "17777KG", "19400KG"},
                {"Wayne Fleming", "West Dannyville", "North Marianview", "4688.26", "2024-05-19", "P49", "Refrigerated", "2020", "15660KG", "17420KG"},
                {"Sara Duncan", "South Tammybury", "Lake Evanland", "2863.74", "2023-09-30", "P56", "Flatbed", "2019", "19877KG", "21015KG"},
                {"Lora Craig", "North Frankton", "East Johnnyshire", "3178.49", "2024-01-26", "P46", "Lowboy", "2022", "18920KG", "20080KG"},
                {"Brett Dunn", "East Mariofort", "Port Noelshire", "3599.12", "2023-12-16", "P53", "Dry Van", "2021", "17280KG", "18560KG"}
        };

        for (String[] row : data) {
            Map<String, Object> record = new HashMap<>();
            record.put("name", row[0]);
            record.put("to", row[1]);
            record.put("from", row[2]);
            record.put("amount", row[3]);
            record.put("date", row[4]);
            record.put("truckName", row[5]);
            record.put("truckType", row[6]);
            record.put("model", row[7]);
            record.put("weight", row[8]);
            record.put("capacity", row[9]);
            records.add(record);
        }
        return records;
    }
}
