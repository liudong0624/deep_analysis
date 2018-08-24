package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetMongoMessage implements Runnable{
    private Logger logger = LoggerFactory.getLogger(GetMongoMessage.class);
    private MongoDb mongoDb;
    private TaskInfo taskInfo;

    public GetMongoMessage(MongoDb mongoDb_, TaskInfo taskInfo_){
        mongoDb = mongoDb_;
        taskInfo = taskInfo_;
    }


    @Override
    public void run() {
        mongoDb.GetFileJson(taskInfo);
    }
}
