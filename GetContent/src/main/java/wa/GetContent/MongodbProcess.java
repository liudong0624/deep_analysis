package wa.GetContent;

import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;

import org.bson.Document;
import org.bson.conversions.Bson;
import utils.MongodbBase;
import static com.mongodb.client.model.Filters.eq;

public class MongodbProcess extends MongodbBase {
    private final String sstv_data_temp = "sensitive_data_temp";
    private MongoCollection<Document> sstv_data_collection;

    public MongodbProcess(){
        super();
        sstv_data_collection = mongoDatabase.getCollection(sstv_data_temp);
    }
    public void UpdateContent(JSONObject task){
        Bson update = Updates.set("content",task.getString("__content"));
        sstv_data_collection.updateOne(eq("md5",task.getString("md5")),update);
    }
}
