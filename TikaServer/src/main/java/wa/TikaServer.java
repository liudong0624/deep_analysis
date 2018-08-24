package wa;

import ConsulConfig.ConsulConfig;
import com.orbitz.consul.Consul;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.grpctika.*;
import io.grpc.stub.StreamObserver;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;


public class TikaServer {
    private static Logger logger = LoggerFactory.getLogger(TikaServer.class);

    private int port;
    private Server server;
    private Tika tika;

    private void init(){
        Consul consul = ConsulConfig.getInstance().GetConsul();
        String strport = consul.keyValueClient().getValueAsString("tika.server.selfport").get();
        if (strport == null)
        {
            logger.error("配置读取失败");
        }
        port = Integer.parseInt(strport);

        ConsulServers.Register.register("TIKA","tika",port);
        tika = new Tika();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        logger.info("Tika服务开启");
        final TikaServer server = new TikaServer();
        server.init();
        server.start();
        server.blockUntilShutdown();
    }

    private void start() throws IOException {
        server = ServerBuilder.forPort(port).
                addService(new TikaServer.TikaImpl())
                .addService(new HealthCheck())
                .build()
                .start();
        logger.info("service start...");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("*** shutting down gRPC server since JVM is shutting down");
            TikaServer.this.stop();
            logger.info("*** server shutdown");
        }));
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    // block 一直到退出程序
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private class TikaImpl extends TikaGrpc.TikaImplBase {
        public void gRPCTikaParseToString(Value req, StreamObserver<ParseResult> responseObserver) {

            Metadata metadata = new Metadata();
            InputStream in = null;
            StringBuilder metadataBuffer = new StringBuilder();
            String parsestring = null;
            try {
                if (!req.getUrl().equals("")) {
                    in = TikaInputStream.get(new URL(req.getUrl()), metadata);
                } else {
                    in = req.getDoctment().newInput();
                    if(!req.getPassword().equals(""))
                    {
                        metadata.set("password", req.getPassword());
                    }
                }
                parsestring = tika.parseToString(in, metadata);
            } catch (Exception e) {
                ParseResult reply = ParseResult.newBuilder().setException(e.toString()).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
                return;
            }

            String[] names = metadata.names();
            Arrays.sort(names);
            for (String name : names) {
                for (String val : metadata.getValues(name)) {
                    metadataBuffer.append(name);
                    metadataBuffer.append(": ");
                    metadataBuffer.append(val);
                    metadataBuffer.append("\n");
                }
            }
            logger.debug("md5:" + req.getMd5());
            logger.debug("content:" + parsestring);
            ParseResult reply = ParseResult.newBuilder().setContent(parsestring).setMeta(metadataBuffer.toString()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void gRPCDetect(Value req, StreamObserver<Typestring> responseObserver) {
            //Metadata metadata = new Metadata();

            InputStream in = null;
            //StringBuilder metadataBuffer = new StringBuilder();
            String typestring = null;
            try {
                if (!req.getUrl().equals("")) {
                    in = TikaInputStream.get(new URL(req.getUrl()));
                } else {
                    in = req.getDoctment().newInput();
                }
                typestring = tika.detect(in);
            } catch (Exception e) {
                Typestring reply = Typestring.newBuilder().setException(e.toString()).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
                return;
            }
            logger.debug("md5:" + req.getMd5());
            logger.debug("type:" + typestring);
            Typestring reply = Typestring.newBuilder().setType(typestring).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

/*
        @Override
        public void check(io.grpc.grpctika.HealthCheckRequest request,
                          io.grpc.stub.StreamObserver<io.grpc.grpctika.HealthCheckResponse> responseObserver) {
            String service = request.getService();
            logger.debug("服务{}查询健康状态",service);
            io.grpc.grpctika.HealthCheckResponse response =
                    io.grpc.grpctika.HealthCheckResponse.newBuilder().
                            setStatusValue(1).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
*/


        @Override
        public void gRPCParseEmbedded(Value request, StreamObserver<EmbeddedList> responseObserver) {
            logger.debug("md5:" + request.getMd5());
            gRPCFileEmbedded fileembedded = new gRPCFileEmbedded();
            EmbeddedList.Builder emlist = EmbeddedList.newBuilder();
            try {
                //解嵌套
                fileembedded.extract(request.getDoctment().newInput(),request.getPassword());

                if (fileembedded.isHaveEmbededDoc()) {
                    emlist.addAllItems(fileembedded.getFileList());
                    fileembedded.clear();
                }

            }catch (Exception e){
                emlist.setException(e.toString());
                responseObserver.onNext(emlist.build());
                responseObserver.onCompleted();
                return;
            }

            responseObserver.onNext(emlist.build());
            responseObserver.onCompleted();
        }
    }
}
