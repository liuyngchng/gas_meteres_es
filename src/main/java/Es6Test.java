//import org.apache.http.HttpHost;
//import org.elasticsearch.ElasticsearchException;
//import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
//import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
//import org.elasticsearch.action.get.GetRequest;
//import org.elasticsearch.action.get.GetResponse;
//import org.elasticsearch.client.RestClient;
//import org.elasticsearch.client.RestHighLevelClient;
//import org.elasticsearch.common.Strings;
//import org.elasticsearch.common.settings.Settings;
//import org.elasticsearch.common.xcontent.XContentType;
//import org.elasticsearch.rest.RestStatus;
//import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
//
//import java.io.IOException;
//import java.util.Map;
//
///**
// * https://blog.csdn.net/qq_26676207/article/details/81019677
// */
//public class Es6Test {
//
//    private String username = "elastic";
//    private String password = "EaPvk=T+6N5j3Wy+QZVR";
//
//    public static void main(String[] args) throws IOException {
//        get();
//    }
//
//    public static void get() {
//        try {
//            RestHighLevelClient client = new RestHighLevelClient(
//                RestClient.builder(
//                    new HttpHost("localhost", 9200, "http")
//                )
//            );
//
//            // 1、创建获取文档请求
//            GetRequest request = new GetRequest(
//                "twitter",   //索引
//                "_doc",     // mapping type
//                "1");     //文档id
//
//            // 2、可选的设置
//            //request.routing("routing");
//            //request.version(2);
//
//            //request.fetchSourceContext(new FetchSourceContext(false)); //是否获取_source字段
//            //选择返回的字段
//            String[] includes = new String[]{"message", "*Date"};
//            String[] excludes = Strings.EMPTY_ARRAY;
//            FetchSourceContext fetchSourceContext = new FetchSourceContext(true, includes, excludes);
//            request.fetchSourceContext(fetchSourceContext);
//
//            //也可写成这样
//                /*String[] includes = Strings.EMPTY_ARRAY;
//                String[] excludes = new String[]{"message"};
//                FetchSourceContext fetchSourceContext = new FetchSourceContext(true, includes, excludes);
//                request.fetchSourceContext(fetchSourceContext);*/
//
//
//            // 取stored字段
//                /*request.storedFields("message");
//                GetResponse getResponse = client.get(request);
//                String message = getResponse.getField("message").getValue();*/
//
//
//            //3、发送请求
//            GetResponse getResponse = null;
//            try {
//                // 同步请求
//                getResponse = client.get(request);
//            } catch (ElasticsearchException e) {
//                if (e.status() == RestStatus.NOT_FOUND) {
//                    System.out.println("没有找到该id的文档" );
//                }
//                if (e.status() == RestStatus.CONFLICT) {
//                    System.out.println("获取时版本冲突了，请在此写冲突处理逻辑！" );
//                }
//                System.out.println(e);
//            }
//
//            //4、处理响应
//            if(getResponse != null) {
//                String index = getResponse.getIndex();
//                String type = getResponse.getType();
//                String id = getResponse.getId();
//                if (getResponse.isExists()) { // 文档存在
//                    long version = getResponse.getVersion();
//                    String sourceAsString = getResponse.getSourceAsString(); //结果取成 String
//                    Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();  // 结果取成Map
//                    byte[] sourceAsBytes = getResponse.getSourceAsBytes();    //结果取成字节数组
//
//                    System.out.println("index:" + index + "  type:" + type + "  id:" + id);
//                    System.out.println(sourceAsString);
//
//                } else {
//                    System.out.println("没有找到该id的文档" );
//                }
//            }
//
//
//            //异步方式发送获取文档请求
//            /*
//            ActionListener<GetResponse> listener = new ActionListener<GetResponse>() {
//                @Override
//                public void onResponse(GetResponse getResponse) {
//
//                }
//
//                @Override
//                public void onFailure(Exception e) {
//
//                }
//            };
//            client.getAsync(request, listener);
//            */
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void put()throws IOException {
//        RestHighLevelClient client = null;
//        try {
//            client = new RestHighLevelClient(
//                RestClient.builder(
//                    new HttpHost("localhost", 9200, "http")));
//            // 1、创建 创建索引request 参数：索引名mess
//            CreateIndexRequest request = new CreateIndexRequest("twitter");
//            // 2、设置索引的settings
//            request.settings(Settings.builder().put("index.number_of_shards", 3) // 分片数
//                    .put("index.number_of_replicas", 1) // 副本数
////                .put("analysis.analyzer.default.tokenizer", "ik_smart") // 默认分词器
//            );
//            // 3、设置索引的mappings
//            String source = "{\n" +
//                "\t\"type\": {\n" +
//                "\t\t\"type\": \"keyword\"\n" +
//                "\t},\n" +
//                "\t\"name\": {\n" +
//                "\t\t\"type\": \"text\"\n" +
//                "\t},\n" +
//                "\t\"user_name\": {\n" +
//                "\t\t\"type\": \"keyword\"\n" +
//                "\t},\n" +
//                "\t\"email\": {\n" +
//                "\t\t\"type\": \"keyword\"\n" +
//                "\t},\n" +
//                "\t\"content\": {\n" +
//                "\t\t\"type\": \"text\"\n" +
//                "\t},\n" +
//                "\t\"tweeted_at\": {\n" +
//                "\t\t\"type\": \"date\"\n" +
//                "\t}\n" +
//                "}";
//            System.out.println(source);
//            request.mapping("properties",source
//                ,
//                XContentType.JSON);
//            // 5、 发送请求
//            // 5.1 同步方式发送请求
//            CreateIndexResponse createIndexResponse = client.indices()
//                .create(request);
//
//            // 6、处理响应
//            boolean acknowledged = createIndexResponse.isAcknowledged();
//            boolean shardsAcknowledged = createIndexResponse
//                .isShardsAcknowledged();
//            System.out.println("acknowledged = " + acknowledged);
//            System.out.println("shardsAcknowledged = " + shardsAcknowledged);
//            // 5.1 异步方式发送请求
//            /*ActionListener<CreateIndexResponse> listener = new ActionListener<CreateIndexResponse>() {
//                @Override
//                public void onResponse(
//                        CreateIndexResponse createIndexResponse) {
//                    // 6、处理响应
//                    boolean acknowledged = createIndexResponse.isAcknowledged();
//                    boolean shardsAcknowledged = createIndexResponse
//                            .isShardsAcknowledged();
//                    System.out.println("acknowledged = " + acknowledged);
//                    System.out.println(
//                            "shardsAcknowledged = " + shardsAcknowledged);
//                }
//
//                @Override
//                public void onFailure(Exception e) {
//                    System.out.println("创建索引异常：" + e.getMessage());
//                }
//            };
//
//            client.indices().createAsync(request, listener);
//            */
//        }
//        catch (Exception ex) {
//            System.out.println(ex);
//
//        }
//        client.close();
//
//    }
//}
