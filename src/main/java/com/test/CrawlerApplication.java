package com.test;

import com.huawei.fd.api.entity.*;
import com.huawei.fd.api.exception.FusionDirectorException;
import com.huawei.fd.api.wrapper.*;
import com.huawei.fd.service.bean.FusionDirector;
import com.huawei.fd.service.bean.GroupBean;
import com.huawei.fd.service.bean.NodeBean;
import com.huawei.fd.util.HttpRequestUtil;
import org.apache.log4j.Logger;
import org.springframework.web.client.ResourceAccessException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by HLuo on 3/7/2019.
 */
public class CrawlerApplication {

    private static final Logger logger = Logger.getLogger(CrawlerApplication.class);

    public static FusionDirector getFusionDirector(){

        try {

            String host, user, code, classifyMethod;

            int port;

            InputStream in = new FileInputStream("fusionDirector.properties");

            Properties p = new Properties();
            p.load(in);
            host = p.getProperty("fd.host");
            user = p.getProperty("fd.user");
            code = p.getProperty("fd.code");
            classifyMethod = p.getProperty("fd.classifyMethod");

            port = Integer.parseInt(p.get("fd.port").toString());

            return new FusionDirector(host, port, user, code, classifyMethod);
        }catch (IOException e) {
            logger.error("Fail to read file fusionDirector.properties",e);
            return null;
        }
    }

    public static void main(String[] args)  {
        FusionDirector fusionDirector = getFusionDirector();
        String jsonDir = System.getProperty("user.dir") +"/fusionDirector_"+fusionDirector.getHost()+"/";

        logger.info("json files will be save to dir: " + jsonDir);

        if(fusionDirector==null){
            logger.error("Fail to read connection from properties file.");
            return;
        }

        logger.info("Host for fusion director is " + fusionDirector.getHost());

        logger.info("==========collecting fusionDirector version");
        {
            AbstractApiWrapper wrapper = new FusionDirectorVersionWrapper(fusionDirector);
            String result = null;
            try {
                result = wrapper.call(String.class);
                saveJsonFile(jsonDir, "fd-version.json", result);

            } catch (FusionDirectorException e) {
                logger.error(e.getMessage(), e);
            } catch (ResourceAccessException e) {
                logger.error(e.getMessage(), e);
            }
        }

        logger.info("==========collecting groups data");

        {
            AbstractApiWrapper wrapper = new GroupListApiWrapper(fusionDirector);

            GroupListEntity result = null;
            try {
                String jsonString = wrapper.call(String.class);
                saveJsonFile(jsonDir+"group/","group_list.json", jsonString);
                result = HttpRequestUtil.json2Object(jsonString, GroupListEntity.class);
            } catch (FusionDirectorException e) {
                logger.error(e.getMessage(), e);
            } catch (ResourceAccessException e) {
                logger.error(e.getMessage(), e);
            }

            for (GroupEntity group : result.getMembers()) {
                //System.out.println(node.getDeviceID());
                AbstractApiWrapper groupApiWrapper = new GroupApiWrapper(fusionDirector);
                groupApiWrapper.setPathVarivable(group.getId());
                try {
//                    groupList.add(groupApiWrapper.call(GroupBean.class));
                    String jsonString = groupApiWrapper.call(String.class);
                    saveJsonFile(jsonDir + "group/","group-"+group.getId()+".json", jsonString);
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                }
            }

        }

        logger.info("==========collecting nodes data");

        {

            AbstractApiWrapper wrapper = new NodeListApiWrapper(fusionDirector);

            List<NodeEntity> resultList = new ArrayList<NodeEntity>();
            int pageSize = 50;
            int start = 0;


            wrapper.addParameter("$top", pageSize+"");
            while(true) {

                try {
                    wrapper.addParameter("$skip", "" + (start * pageSize));
                    String jsonString = wrapper.call(String.class);
                    saveJsonFile(jsonDir+"nodes/", "node-list-"+start+".json", jsonString);
                    start++;
                    NodeListEntity entity =  HttpRequestUtil.json2Object(jsonString, NodeListEntity.class);
                    resultList.addAll(entity.getMembers());
                    if(entity.hasMoreEntry()==false){
                        break;
                    }
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                } catch (ResourceAccessException e) {
                    logger.error(e.getMessage(), e);
                    return;
                }
            }

            for (NodeEntity node : resultList) {
                AbstractApiWrapper nodeApiWrapper = new NodeApiWrapper(fusionDirector);
                nodeApiWrapper.setPathVarivable(node.getDeviceID());

                NodeBean nodeBean = null;
                try {
                    String jsonString = nodeApiWrapper.call(String.class);
                    saveJsonFile(jsonDir+"nodes/", "node-"+node.getDeviceID()+".json", jsonString);
                    nodeBean = HttpRequestUtil.json2Object(jsonString, NodeBean.class);

                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                    continue;
                }

                AbstractApiWrapper catalogApiWrapper = new CatalogueApiWrapper(fusionDirector);
                catalogApiWrapper.setPathVarivable(node.getDeviceID());

                CatalogueEntity catalog = null;
                try {
                    String jsonString = catalogApiWrapper.call(String.class);
                    saveJsonFile(jsonDir+"nodes/", "node-"+node.getDeviceID()+"-catalogue.json", jsonString);
//                    catalog = HttpRequestUtil.json2Object(jsonString, CatalogueEntity.class);
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                }

                AbstractApiWrapper statisticApiWrapper = new NodeStatisticsApiWrapper(fusionDirector);
                statisticApiWrapper.setPathVarivable(node.getDeviceID());

                NodeStatisticsEntity statistic = null;
                try {
                    String jsonString = statisticApiWrapper.call(String.class);
                    saveJsonFile(jsonDir+"nodes/", "node-"+node.getDeviceID()+"-statistics.json", jsonString);
//                    statistic =  HttpRequestUtil.json2Object(jsonString, NodeStatisticsEntity.class); statisticApiWrapper.call(NodeStatisticsEntity.class);
//                    nodeBean.setTemperature(statistic.getOutletTemperature());
//                    nodeBean.setFanSpeedLevel(statistic.getFanSpeedLevel());
//                    nodeBean.setPowerConsumed(statistic.getPowerConsumed());
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                }

                //Processor
                AbstractApiWrapper processApiWrapper = new ProcessorListApiWrapper(fusionDirector);
                processApiWrapper.setPathVarivable(node.getDeviceID());
//                ProcessorListEntity processorListEntity = null;
                try {
//                    processorListEntity = processApiWrapper.call(ProcessorListEntity.class);
                    String jsonString = processApiWrapper.call(String.class);
                    saveJsonFile(jsonDir+"nodes/", "node-"+node.getDeviceID()+"-processor.json", jsonString);
//                    GroupResourceBean processorGroup = new GroupResourceBean("processorGroup", "Processor Group");
//                    processorGroup.addChildren(processorListEntity.getMembers());
//                    if (catalog !=null) {
//                        processorGroup.setHealthStatus(catalog.getProcessorHealth());
//                    }
//                    nodeBean.addChild(processorGroup);
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                }

                //drive
                AbstractApiWrapper driveListApiWrapper = new DriveListApiWrapper(fusionDirector);
                driveListApiWrapper.setPathVarivable(node.getDeviceID());
                DriveListEntity driveListEntity = null;
//                GroupResourceBean storageGroup = new GroupResourceBean("storageGroup", "Storage Group");

                try{
                    String jsonString = driveListApiWrapper.call(String.class);
                    saveJsonFile(jsonDir+"nodes/", "node-"+node.getDeviceID()+"-drive.json", jsonString);
                    driveListEntity = HttpRequestUtil.json2Object(jsonString, DriveListEntity.class);
//                    if (catalog !=null) {
//                        storageGroup.setHealthStatus(catalog.getStorageHealth());
//                    }

                    for (DriveEntity entity : driveListEntity.getMembers()) {
                        AbstractApiWrapper driveApiWrapper = new DriveApiWrapper(fusionDirector);
                        driveApiWrapper.setPathVarivable(node.getDeviceID(), entity.getDeviceID());

                        String jsonStringBean = driveApiWrapper.call(String.class);
                        saveJsonFile(jsonDir+"nodes/", "drive-"+entity.getDeviceID()+".json", jsonStringBean);
//                        storageGroup.addChild(driveBean);

                    }

//                    nodeBean.addChild(storageGroup);

                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                }

                //Raid card
                AbstractApiWrapper raidCardListApiWrapper = new RaidCardListApiWrapper(fusionDirector);
                raidCardListApiWrapper.setPathVarivable(node.getDeviceID());

                RaidCardListEntity raidCardListEntity =null;
                try{
                    String jsonString = raidCardListApiWrapper.call(String.class);
                    saveJsonFile(jsonDir+"nodes/", "node-"+node.getDeviceID()+"-raidCard.json", jsonString);
                    raidCardListEntity = HttpRequestUtil.json2Object(jsonString, RaidCardListEntity.class);//raidCardListApiWrapper.call(RaidCardListEntity.class);

                    for (RaidCardEntity entity : raidCardListEntity.getMembers()) {
                        AbstractApiWrapper raidCardApiWrapper = new RaidCardApiWrapper(fusionDirector);
                        raidCardApiWrapper.setPathVarivable(node.getDeviceID(), entity.getDeviceID());
//                        RaidCardBean raidCardBean = raidCardApiWrapper.call(RaidCardBean.class);

                        String jsonStringBean = raidCardApiWrapper.call(String.class);
                        saveJsonFile(jsonDir+"nodes/", "raidCard-"+entity.getDeviceID()+".json", jsonStringBean);

//                        String name = raidCardBean.getName();
//
//                        if (name == null) {
//                            logger.error("Raid card name is empty, check url to find problematic raid card data: " + raidCardApiWrapper.getRequestURL());
//                            continue;
//                        }
//
//                        if (name.startsWith("RAIDStorage")) {
//                            for (StorageControllerBean controllerBean : raidCardBean.getStorageControllers()){
//
//                                controllerBean.setDeviceID(raidCardBean.getDeviceID());
////                    		storageGroup.addChild(raidCardBean);
//                                storageGroup.addChild(controllerBean);
//                            }
//                        }
                    }
//                nodeBean.addChild(raidCardGroup);
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                }

                //Memory
                AbstractApiWrapper memoryListApiWrapper = new MemoryListApiWrapper(fusionDirector);
                memoryListApiWrapper.setPathVarivable(node.getDeviceID());
                MemoryListEntity memoryListEntity = null;
                try {
//                    memoryListEntity = memoryListApiWrapper.call(MemoryListEntity.class);

                    String jsonString = memoryListApiWrapper.call(String.class);
                    saveJsonFile(jsonDir+"nodes/", "node-"+node.getDeviceID()+"-memory.json", jsonString);

//                    GroupResourceBean memoryGroup = new GroupResourceBean("memoryGroup", "Memory Group");
//                    if (catalog !=null) {
//                        memoryGroup.setHealthStatus(catalog.getMemoryHealth());
//                    }

//                    memoryGroup.addChildren(memoryListEntity.getMembers());
//                    nodeBean.addChild(memoryGroup);
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                }

                //PCIE
                AbstractApiWrapper pcieListApiWrapper = new PCIEListApiWrapper(fusionDirector);
                pcieListApiWrapper.setPathVarivable(node.getDeviceID());
                PCIEListEntity pcieListEntity = null;
                try{
                    String jsonString = pcieListApiWrapper.call(String.class);
                    saveJsonFile(jsonDir+"nodes/", "node-"+node.getDeviceID()+"-pcie.json", jsonString);
                    pcieListEntity = HttpRequestUtil.json2Object(jsonString, PCIEListEntity.class);//pcieListApiWrapper.call(PCIEListEntity.class);
//                    GroupResourceBean pcieGroup = new GroupResourceBean("pcieGroup", "PCIE Group");
                    for (PCIEEntity entity : pcieListEntity.getMembers()) {
                        AbstractApiWrapper pcieApiWrapper = new PCIEApiWrapper(fusionDirector);
                        pcieApiWrapper.setPathVarivable(node.getDeviceID(), entity.getDeviceID());
//                        PCIEBean pcieBean = pcieApiWrapper.call(PCIEBean.class);
                        String jsonStringBean = pcieApiWrapper.call(String.class);
                        saveJsonFile(jsonDir+"nodes/", "pcie-"+entity.getDeviceID()+".json", jsonStringBean);
//                        pcieGroup.addChild(pcieBean);
                    }
//                    nodeBean.addChild(pcieGroup);
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                }

                //Power
                AbstractApiWrapper powerApiWrapper = new PowerListApiWrapper(fusionDirector);
                powerApiWrapper.setPathVarivable(node.getDeviceID());
                PowerListEntity powerListEntity = null;
                try{
//                    powerListEntity = powerApiWrapper.call(PowerListEntity.class);
                    String jsonString = powerApiWrapper.call(String.class);
                    saveJsonFile(jsonDir+"nodes/", "node-"+node.getDeviceID()+"-power.json", jsonString);
//                    GroupResourceBean powerGroup = new GroupResourceBean("powerGroup", "Power Group");
//                    if (catalog !=null) {
//                        powerGroup.setHealthStatus(catalog.getPowerHealth());
//                    }
//                    powerGroup.addChildren(powerListEntity.getMembers());
//                    nodeBean.addChild(powerGroup);
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                }

                //Thermal
                AbstractApiWrapper fanApiWrapper = new FanListApiWrapper(fusionDirector);
                fanApiWrapper.setPathVarivable(node.getDeviceID());

                FanListEntity fanListEntity = null;
                try{
//                    fanListEntity = fanApiWrapper.call(FanListEntity.class);
                    String jsonString = fanApiWrapper.call(String.class);
                    saveJsonFile(jsonDir+"nodes/", "node-"+node.getDeviceID()+"-thermal.json", jsonString);
//                    GroupResourceBean fanGroup = new GroupResourceBean("fanGroup", "Fan Group");
//                    if (catalog !=null) {
//                        fanGroup.setHealthStatus(catalog.getFanHealth());
//                    }

//                    fanGroup.addChildren(fanListEntity.getMembers());
//                    nodeBean.addChild(fanGroup);
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                }

                //NetworkAdapter
                AbstractApiWrapper networkAdapterListApiWrapper = new NetworkAdapterListApiWrapper(fusionDirector);
                networkAdapterListApiWrapper.setPathVarivable(node.getDeviceID());
                NetworkAdapterListEntity networkAdapterListEntity = null;
                try{
                    String jsonString = networkAdapterListApiWrapper.call(String.class);
                    saveJsonFile(jsonDir+"nodes/", "node-"+node.getDeviceID()+"-networkAdapter.json", jsonString);

                    networkAdapterListEntity = HttpRequestUtil.json2Object(jsonString, NetworkAdapterListEntity.class);//networkAdapterListApiWrapper.call(NetworkAdapterListEntity.class);
//                    GroupResourceBean networkAdapterGroup = new GroupResourceBean("networkAdapterGroup", "Network Adapter Group");
                    for (NetworkAdapterEntity entity : networkAdapterListEntity.getMembers()) {
                        AbstractApiWrapper networkAdapterApiWrapper = new NetworkAdapterApiWrapper(fusionDirector);
                        networkAdapterApiWrapper.setPathVarivable(node.getDeviceID(), entity.getDeviceID());

//                        NetworkAdapterBean networkAdapterBean = networkAdapterApiWrapper.call(NetworkAdapterBean.class);
                        String jsonStringBean = networkAdapterApiWrapper.call(String.class);
                        saveJsonFile(jsonDir+"nodes/", "networkAdapter-"+entity.getDeviceID()+".json", jsonStringBean);

//                        networkAdapterGroup.addChild(networkAdapterBean);
                    }
//                    nodeBean.addChild(networkAdapterGroup);
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                }

//                nodeList.add(nodeBean);

            }
        }

        logger.info("==========collecting enclosures data");

        {

            AbstractApiWrapper enclosureListApiWrapper = new EnclosureListApiWrapper(fusionDirector);
            List<EnclosureEntity> results = new ArrayList<EnclosureEntity>();

            int pageSize = 50;
            int start = 0;


            enclosureListApiWrapper.addParameter("$top", pageSize+"");
            while(true) {

                try {
                    enclosureListApiWrapper.addParameter("$skip", "" + (start * pageSize));
                    String jsonString = enclosureListApiWrapper.call(String.class);
                    saveJsonFile(jsonDir+"enclosure/", "enclosure-list-"+start+".json", jsonString);
                    start++;
//                    results = enclosureListApiWrapper.callList(EnclosureListEntity.class);
                    EnclosureListEntity enclosureListEntity = HttpRequestUtil.json2Object(jsonString, EnclosureListEntity.class);
                    results.addAll(enclosureListEntity.getMembers());
                    if(enclosureListEntity.hasMoreEntry()==false){
                        break;
                    }
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                } catch (ResourceAccessException e) {
                    logger.error(e.getMessage(), e);

                }
            }

            for (EnclosureEntity entity : results) {
                AbstractApiWrapper enclosureApiWrapper = new EnclosureApiWrapper(fusionDirector);
                enclosureApiWrapper.setPathVarivable(entity.getDeviceID());
                try {
                    String jsonString = enclosureApiWrapper.call(String.class);
                    saveJsonFile(jsonDir+"enclosure/", "enclosure-"+entity.getDeviceID()+".json", jsonString);
//                    EnclosureBean enclosureBean = enclosureApiWrapper.call(EnclosureBean.class);
//                    enclosureBean.setDeviceID(entity.getDeviceID());
//                    enclosureList.add(enclosureBean);
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        logger.info("==========collecting switch node data");

        {
            AbstractApiWrapper wrapper = new SwitchNodeListApiWrapper(fusionDirector);

            List<SwitchNodeEntity> results = new ArrayList<SwitchNodeEntity>();

            int pageSize = 50;
            int start = 0;


            wrapper.addParameter("$top", pageSize+"");
            while(true) {
                try {

                    wrapper.addParameter("$skip", "" + (start * pageSize));
                    String jsonString = wrapper.call(String.class);
                    saveJsonFile(jsonDir+"switch-nodes/", "switch-node-list-"+start+".json", jsonString);
                    start++;
                    SwitchNodeListEntity switchNodeListEntity = HttpRequestUtil.json2Object(jsonString,SwitchNodeListEntity.class);//wrapper.callList(SwitchNodeListEntity.class);
                    results.addAll(switchNodeListEntity.getMembers());
                    if(switchNodeListEntity.hasMoreEntry()==false){
                        break;
                    }
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                } catch (ResourceAccessException e) {
                    logger.error(e.getMessage(), e);
                }
            }

            for (SwitchNodeEntity entity : results) {
                AbstractApiWrapper switchNodeApiWrapper = new SwitchNodeApiWrapper(fusionDirector);
                switchNodeApiWrapper.setPathVarivable(entity.getDeviceID());
                try {

                    String jsonString = switchNodeApiWrapper.call(String.class);
                    saveJsonFile(jsonDir+"switch-nodes/", "switch-node-"+entity.getDeviceID()+".json", jsonString);

//                    SwitchNodeBean switchNodeBean = switchNodeApiWrapper.call(SwitchNodeBean.class);
                    //Board
//                    AbstractApiWrapper boardListApiWrapper = new BoardListApiWrapper(fusionDirector);
//                    boardListApiWrapper.setPathVarivable(switchNodeBean.getDeviceID());

//                    switchNodeList.add(switchNodeBean);
                } catch (FusionDirectorException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        logger.info("DONE!");

    }

    public static void saveJsonFile(String dir, String fileName, String content){

        String fullPath = dir+fileName;

        FileWriter fileWriter = null;
        try {
            File tempDir = new File( dir);
            if(tempDir.exists()==false){
                tempDir.mkdirs();
            }

            fileWriter = new FileWriter(dir + fileName);
            fileWriter.write(content);
            logger.info("Json file '"+dir + fileName+"' saved");
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                logger.error("save json to file '"+fullPath+"' failed.", e);
            }
        }


    }
}
