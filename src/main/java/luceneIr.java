import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.poi.hslf.extractor.PowerPointExtractor;

import org.apache.poi.hwpf.extractor.WordExtractor;


import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;


import org.wltea.analyzer.lucene.IKAnalyzer;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class luceneIr {
    /***
     * 创建索引
     * @param targetFileDir 源文件夹
     * @param indexSaveDir 索引存放文件夹
     * @throws IOException
     */
    public static void indexCreate(File targetFileDir, File indexSaveDir) throws IOException, InvalidFormatException {
        // 不是目录或不存在则返回
        if (!targetFileDir.isDirectory() || !targetFileDir.exists()) {
            return;
        }
        // 保存Lucene索引文件的路径
        Directory directory = FSDirectory.open(indexSaveDir.toPath());
        // 创建一个简单的分词器,可以对数据进行分词
        Analyzer analyzer = new StandardAnalyzer();
        // 创建索引实例
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
        // 获取所有需要建立索引的文件
        File[] files = targetFileDir.listFiles();

        for (int i = 0; i < files.length; i++) {
            // 文件的完整路径 files[i].toString()
            // 获取文件名称
            String fileName = files[i].getName();
            // 获取文件后缀名，将其作为文件类型
            String fileType = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length()).toLowerCase();
            if(fileType.equals("index")){
                continue;
            }

            Document doc = new Document();
            InputStream in = new FileInputStream(files[i]);
            FieldType fieldType = new FieldType();
            fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
            fieldType.setStored(true);
            fieldType.setTokenized(true);

            // 为每种文档类型创建索引
            if (!fileType.equals("")) {
                switch (fileType) {
                    // doc类型文档
                    case "doc":
                        // 获取doc的word文档
                        WordExtractor wordExtractor = new WordExtractor(in);
                        // 创建Field对象，并放入doc对象中
                        doc.add(new Field("contents", wordExtractor.getText(), fieldType));
                        // 关闭文档
                        wordExtractor.close();
                        break;

                    // docx类型文档
                    case "docx":
                        // 获取docx的word文档
                        XWPFWordExtractor xwpfWordExtractor = new XWPFWordExtractor(new XWPFDocument(in));
                        // 创建Field对象，并放入doc对象中
                        doc.add(new Field("contents", xwpfWordExtractor.getText(), fieldType));
                        // 关闭文档
                        xwpfWordExtractor.close();
                        break;

                    // pdf类型文档
                    case "pdf":
                        // 获取pdf文档
                        PDFParser parser = new PDFParser(in);
                        parser.parse();
                        PDDocument pdDocument = parser.getPDDocument();
                        PDFTextStripper stripper = new PDFTextStripper();
                        // 创建Field对象，并放入doc对象中
                        doc.add(new Field("contents", stripper.getText(pdDocument), fieldType));
                        // 关闭文档
                        pdDocument.close();
                        break;

                    // txt类型文档
                    case "txt":
                        String encodingTxt = "GBK";
                        String txtFile = FileUtils.readFileToString(files[i],encodingTxt);
                        // 创建Field对象，并放入doc对象中
                        //System.out.println(txtFile);
                        System.out.println(txtFile);
                        doc.add(new Field("contents", txtFile, fieldType));
                        //System.out.println(doc);
                        break;

                    // html类型文档
                    case "html": {
                        StringBuffer sb = new StringBuffer();
                        String str = null;
                        String encoding = "GBK";
                        File file = new File(String.valueOf(files[i]));
                        if (file.isFile() && file.exists()) { //判断文件是否存在
                            InputStreamReader read = new InputStreamReader(
                                    new FileInputStream(file), encoding);//考虑到编码格式
                            BufferedReader bufferedReader = new BufferedReader(read);
                            String lineTxt = null;
                            while ((lineTxt = bufferedReader.readLine()) != null) {
                                sb.append(lineTxt);
                            }
                            str = sb.substring(sb.indexOf("<body>") + 6, sb.indexOf("</body>"));
                            read.close();
                        }
                        doc.add(new Field("contents", str, fieldType));
                        break;
                    }

                    // xml类型文档
                    case "xml": {
                        StringBuffer sb = new StringBuffer();
                        String str = null;
                        String encoding = "UTF-8";
                        File file = new File(String.valueOf(files[i]));
                        if (file.isFile() && file.exists()) { //判断文件是否存在
                            InputStreamReader read = new InputStreamReader(
                                    new FileInputStream(file), encoding);//考虑到编码格式
                            BufferedReader bufferedReader = new BufferedReader(read);
                            String lineTxt = null;
                            while ((lineTxt = bufferedReader.readLine()) != null) {
                                sb.append(lineTxt);
                            }
                            str = sb.substring(sb.indexOf("content"), sb.indexOf("</content>"));
                            read.close();
                        }
                        doc.add(new Field("contents", str, fieldType));
                        break;
                    }

                    // xls类型文档
                    case "xls": {
                        // 工作表
                        Workbook workbook = null;
                        workbook = WorkbookFactory.create(in);
                        StringBuffer sb = new StringBuffer();
                        // 表个数。
                        int numberOfSheets = workbook.getNumberOfSheets();
                        for (int k = 0; k < numberOfSheets; k++) {
                            Sheet sheet = workbook.getSheetAt(k);
                            // 行数。
                            int rowNumbers = sheet.getLastRowNum() + 1;
                            // Excel第一行。
                            Row temp = sheet.getRow(0);
                            if (temp == null) {
                                continue;
                            }
                            int cells = temp.getPhysicalNumberOfCells();
                            // 读数据。
                            for (int row = 0; row < rowNumbers; row++) {
                                Row r = sheet.getRow(row);
                                for (int col = 0; col < cells; col++) {
                                    if (r.getCell(col) == null) {
                                        continue;
                                    }
                                    sb.append(r.getCell(col).toString()).append(" ");
                                }
                            }
                        }
                        doc.add(new Field("contents", String.valueOf(sb), fieldType));
                        workbook.close();
                        break;
                    }

                    // ppt类型文档
                    case "ppt":
                        PowerPointExtractor powerpointExtractor = new PowerPointExtractor(in);
                        doc.add(new Field("contents", powerpointExtractor.getText(), fieldType));
                        break;

                    // 其他非需求类型文档
                    default:
                        System.out.println("文件类型格式错误！！！");
                        continue;
                }
            }
            // 创建文件名的域，并放入doc对象中
            doc.add(new Field("filename", files[i].getName(), fieldType));
            // 创建时间的域，并放入doc对象中
            doc.add(new Field("indexDate", DateTools.dateToString(new Date(), DateTools.Resolution.DAY), fieldType));
            // 写入IndexWriter
            indexWriter.addDocument(doc);
        }
        // 查看IndexWriter里面有多少个索引
        System.out.println("查看IndexWriter里面有多少个索引:" + indexWriter.numDocs());
        // 关闭索引
        indexWriter.close();
    }

    /**
     * 删除指定索引库下面的所有 索引数据
     * @param indexDir
     */
    public static void indexDelAll(File indexDir) throws IOException {
        if (indexDir == null || !indexDir.exists() || indexDir.isFile()) {
            return;
        }
        // 创建 IKAnalyzer 中文分词器
        Analyzer analyzer = new IKAnalyzer();
        Directory directory = FSDirectory.open(indexDir.toPath());
        // 创建 索引写配置对象，传入分词器
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        // 创建 索引写对象
        IndexWriter indexWriter = new IndexWriter(directory, config);
        /** 删除所有索引
         * 如果索引库中的索引已经被删除，则重复删除时无效*/
        indexWriter.deleteAll();
        /** 虽然不 commit，也会生效，但建议做提交操作，*/
        indexWriter.commit();
        /**  关闭流，里面会自动 flush*/
        indexWriter.close();
    }

    /**
     *
     * @param indexPath 索引目录
     * @param searchStr 拆词后的字符集合
     * @param limit 查询条数
     * @throws IOException
     * @return
     */
    public static ArrayList<String> indexSearch(String indexPath, List<String> searchStr, Integer limit) throws IOException {
        if (limit == null)limit=100;
        Directory directory = FSDirectory.open(Paths.get(indexPath));
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        System.out.println(searchStr);
        if (searchStr.size() == 0) {
            return null;
        }
        String[] terms = new String[searchStr.size()];
        for (int i = 0; i < searchStr.size(); i++){
            terms[i] = searchStr.get(i);
        }
        PhraseQuery phraseQuery = new PhraseQuery("contents", terms);
        TopDocs topDocs = indexSearcher.search(phraseQuery, limit); // 前10条

        ArrayList<String> list = new ArrayList<String>();
        // 结果总数topDocs.totalHits
        for (ScoreDoc sdoc : topDocs.scoreDocs) {
            // 根据文档id取存储的文档
            Document hitDoc = indexSearcher.doc(sdoc.doc);
            // 取文档的字段
            list.add(hitDoc.get("filename"));
            System.out.println(hitDoc.get("filename"));
        }
        // 使用完毕，关闭、释放资源
        indexReader.close();
        directory.close();
        return list;
    }
    /***
     *
     * @param ts 需要拆词的字符串
     * @return
     * @throws IOException
     */
    public static List<String> doToken(TokenStream ts) throws IOException {
        List<String> stringList = new ArrayList<>();
        ts.reset();
        CharTermAttribute cta = ts.getAttribute(CharTermAttribute.class);
        while (ts.incrementToken()) {
            stringList.add(cta.toString());
        }
        System.out.println();
        ts.end();
        ts.close();
        return stringList;
    }
    /***
     *
     * @param
     * @return fieldname
     * @throws
     */
    public static String getFieldName(){
        Scanner sc = new Scanner(System.in);
        System.out.println(" Please Enter fieldname:");
        String fieldname = sc.nextLine();  //读取字符串型输入
        return fieldname;

    }
    /***
     *
     * @param
     * @return text
     * @throws
     */
    public static String getText(){
        Scanner sc = new Scanner(System.in);
        System.out.println(" Please Enter text:");
        String text = sc.nextLine();  //读取字符串型输入
        return text;
    }

    public static void main(String[] args) throws IOException{
        try (Analyzer ik = new StandardAnalyzer()) {
            String fileName = getFieldName();
            String t = getText();

            List<String> list = doToken(ik.tokenStream(fileName, t));
            // List<String> list = doToken(ik.tokenStream("", "北邮"));
            File targetFileDir = new File("D:\\Java\\IR\\docs");
            File file=new File("D:\\Java\\IR\\index");
            if(!file.exists()){//如果文件夹不存在
                file.mkdir();//创建文件夹
            }

            File indexSaveDir = new File("D:\\Java\\IR\\index");
//            File indexSaveDir = new File("D:\\Java\\IR\\docs"+"\\index");
            indexCreate(targetFileDir,indexSaveDir);
            indexSearch("D:\\Java\\IR\\index", list, null);
            indexDelAll(new File("D:\\Java\\IR\\index"));
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        }
    }
}
