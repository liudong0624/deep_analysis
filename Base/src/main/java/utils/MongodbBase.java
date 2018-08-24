package utils;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

public class MongodbBase {
    private static Logger logger = LoggerFactory.getLogger(MongodbBase.class);
    protected MongoClient mongoClient;

    protected MongoDatabase mongoDatabase;

    protected final String databaseName = "DataCenter";
    public MongodbBase(){
        mongoClient = Mongodbhandle.getInstance().getMongoClient();
        mongoDatabase = mongoClient.getDatabase(databaseName);
    }

    public class FileResult{
        public FileResult(InputStream in_, GridFSFile gridFSFile_){
            in = in_;
            gridFSFile = gridFSFile_;
        }
        public InputStream in;
        public GridFSFile gridFSFile;
    }
    public MongodbBase.FileResult GetFileStreamAndInfo(String bucketName, String md5){
        GridFSBucket gridFSBucket =  GridFSBuckets.create(mongoDatabase,bucketName);

        GridFSFindIterable it = gridFSBucket.find(
                eq("md5",md5));

        final ObjectId[] id = new ObjectId[1];
        final GridFSFile[] datas = new GridFSFile[1];

        //这里只可能有一个数据被检索到
        Block<GridFSFile> block = gridFSFile -> {
            id[0] = gridFSFile.getObjectId();
            datas[0] = gridFSFile;

            logger.debug("从mongodb取得数据girdfile对象: {}",datas[0].toString());
            //logger.debug("从mongodb取得数据: 元数据{}" ,datas[0].getMetadata().toJson());
        };

        for (GridFSFile fsFile : it) {
            block.apply(fsFile);
        }
        if (id[0] == null)
            return null;
        return new MongodbBase.FileResult(gridFSBucket.openDownloadStream(id[0]),datas[0]);
    }

    public void UpdateOneFileContentType(String bucketName,String md5,String type){
        StringBuilder file_collection = new StringBuilder();
        file_collection.append(bucketName).append(".files");
        MongoCollection<Document> file_Collection = mongoDatabase.getCollection(file_collection.toString());
        file_Collection.updateOne(eq("md5",md5),set("contentType",type));
    }

    public void init(){

    }
}
