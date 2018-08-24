package wa;

import com.google.protobuf.ByteString;
import io.grpc.grpctika.EmbeddedFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.extractor.ParsingEmbeddedDocumentExtractor;
import org.apache.tika.io.IOUtils;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.PasswordProvider;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;


public class gRPCFileEmbedded {
    private Parser parser = new AutoDetectParser();
    PDFParserConfig pdfParserConfig = new PDFParserConfig();
    private Detector detector = ((AutoDetectParser) parser).getDetector();
    private TikaConfig config = TikaConfig.getDefaultConfig();
    private ParseContext parsecontext = null;
    private MyEmbeddedDocumentExtractor extractor = null;

    public gRPCFileEmbedded(){
        parsecontext = new ParseContext();
        pdfParserConfig.setExtractInlineImages(true);
        parsecontext.set(PDFParserConfig.class, pdfParserConfig);
        parsecontext.set(Parser.class, parser);

        extractor = new MyEmbeddedDocumentExtractor(parsecontext);
        parsecontext.set(EmbeddedDocumentExtractor.class, extractor);
    }

    public void clear(){
        //清除extractor中的上次的内容
        extractor.clear();
    }

    public boolean isHaveEmbededDoc(){
        return extractor.isHaveEmbededDoc();
    }

    public List<EmbeddedFile> getFileList(){
        return extractor.getFileList();
    }

    public void extract(InputStream inputStream,String password) throws SAXException,
            TikaException, IOException {
        Metadata metadata = new Metadata();
        ContentHandler contenthandler = new BodyContentHandler(-1);
        //time:2018.08.08
        //auther:liu
        //如果有密码，将密码添加到context
        if(password != null && !password.equals("")) {
            parsecontext.set(PasswordProvider.class, new PasswordProvider() {
                @Override
                public String getPassword(Metadata metadata) {
                    return password;
                }
            });
        }
        parser.parse(inputStream, contenthandler, metadata, parsecontext);
    }

    private class MyEmbeddedDocumentExtractor extends
            ParsingEmbeddedDocumentExtractor {

        private Logger logger = LoggerFactory.getLogger(MyEmbeddedDocumentExtractor.class);

        private int fileCount = 0;

        List<EmbeddedFile> list = new ArrayList<EmbeddedFile>();


        private MyEmbeddedDocumentExtractor(ParseContext context) {
            super(context);
        }

        public boolean shouldParseEmbedded(Metadata metadata) {
            String contenttypeString = metadata.get("Content-Type");
            if (contenttypeString == null) {
                return true;
            }
            if (contenttypeString.matches("image/x-emf")) {
                return false;
            }
            return true;
        }

        public boolean isHaveEmbededDoc(){
            if (list.isEmpty()) {
                return false;
            }
            return true;
        }

        public List<EmbeddedFile> getFileList(){
            return list;
        }

        public void clear(){
            list.clear();
        }

        public void parseEmbedded(InputStream inputstream,
                                  ContentHandler handler, Metadata metadata, boolean outputHtml)
                throws SAXException, IOException {

            String name = metadata.get(Metadata.RESOURCE_NAME_KEY);
            if (name == null) {
                name = "file_" + fileCount++;
            } else {
                name = FilenameUtils.normalize(FilenameUtils.getName(name));
            }
            //
            //name = new String(name.getBytes("iso-8859-1"), "utf-8");
            MediaType contentType = detector.detect(inputstream, metadata);
            logger.debug(String.format("嵌套mime为：%s",contentType.toString()));

            logger.debug("嵌套文件名为："+name);
            // 将stream写入文件夹，这里要将stream记录下来，然后要发送到mq
            // 记录有没有嵌套文件，几个，每个对应的filename，inputstream

            OutputStream outputStream = new ByteArrayOutputStream();
            try {
                if (inputstream instanceof TikaInputStream) {
                    TikaInputStream tin = (TikaInputStream) inputstream;

                    if (tin.getOpenContainer() != null
                            && tin.getOpenContainer() instanceof DirectoryEntry) {
                        POIFSFileSystem fs = new POIFSFileSystem();// 这里的东西不太懂啊！！！！应该是添加文件输出后才失败的
                        copy((DirectoryEntry) tin.getOpenContainer(),
                                fs.getRoot());
                        fs.writeFilesystem(outputStream);
                        fs.close();
                    } else {
                        IOUtils.copy(inputstream, outputStream);
                    }
                } else {
                    IOUtils.copy(inputstream, outputStream);
                }
            } catch (Exception e) {
                logger.error(String.valueOf(e));
            }
            EmbeddedFile file = EmbeddedFile.newBuilder().setFilename(name)
                    .setFilecontent(ByteString.copyFrom(((ByteArrayOutputStream) outputStream).toByteArray())).build();
            list.add(file);
        }

        protected void copy(DirectoryEntry sourceDir, DirectoryEntry destDir)
                throws IOException {
            for (org.apache.poi.poifs.filesystem.Entry entry : sourceDir) {
                if (entry instanceof DirectoryEntry) {

                    DirectoryEntry newDir = destDir.createDirectory(entry
                            .getName());
                    copy((DirectoryEntry) entry, newDir);
                } else {
                    InputStream contents = new DocumentInputStream((DocumentEntry) entry);
                    destDir.createDocument(entry.getName(), contents);
                }
            }
        }
    }
}