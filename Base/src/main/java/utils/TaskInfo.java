package utils;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;

import static java.lang.Thread.NORM_PRIORITY;

public class TaskInfo {
    private Logger logger = LoggerFactory.getLogger(TaskInfo.class);

    private ArrayBlockingQueue<JSONObject> TaskQueue =
            new ArrayBlockingQueue<JSONObject>(NORM_PRIORITY);

    public TaskInfo(){}
    public void SetTask(JSONObject task){
        try {
            TaskQueue.put(task);
            logger.debug("插入队列完毕taskinfo");
        } catch (InterruptedException e) {
            PrintException.Print(logger,e,"队列插入错误 ");
        }
    }
    public JSONObject GetTask(){
        JSONObject jsonObject = null;
        try {
            jsonObject = TaskQueue.take();
            logger.debug("取出队列完毕taskinfo");
        } catch (InterruptedException e) {
            PrintException.Print(logger,e,"队列取出错误");
        }
        return jsonObject;
    }
}
