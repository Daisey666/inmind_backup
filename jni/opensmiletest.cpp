#include <string.h>
#include <jni.h>

#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>

#include <core/smileCommon.hpp>
#include <core/configManager.hpp>
#include <core/commandlineParser.hpp>
#include <core/componentManager.hpp>

#define MODULE "SMILExtract"
#include  <signal.h>
//cComponentManager *cmanGlob = NULL;
//void INThandler(int);
int ctrlc = 0;
/*
void INThandler(int sig)
{
  signal(sig, SIG_IGN);
  if (cmanGlob != NULL) cmanGlob->requestAbort();
  signal(SIGINT, INThandler);
  ctrlc = 1;
}
*/

void error(const char *msg)
{
    perror(msg);
    exit(1);
}

extern "C" {
    JNIEXPORT jstring JNICALL
    //Java_com_inMind_inMindAgent_opensmileManager_sockettest(JNIEnv *env, jobject thiz){
    Java_com_example_tingyao_voicetest_opensmileManager_sockettest(JNIEnv *env, jobject thiz){
        int sockfd, newsockfd, portno;
        socklen_t clilen;
        char buffer[256];
        char term = 'a';
        struct sockaddr_in serv_addr, cli_addr;
        int n;
        sockfd = socket(AF_INET, SOCK_STREAM, 0);
        bzero((char *) &serv_addr, sizeof(serv_addr));
        portno = 5000;
        SMILE_PRINT("first print something\n");
        serv_addr.sin_family = AF_INET;
        serv_addr.sin_addr.s_addr = INADDR_ANY;
        serv_addr.sin_port = htons(portno);
        if (bind(sockfd, (struct sockaddr *) &serv_addr,
              sizeof(serv_addr)) < 0)
              error("ERROR on binding");
        listen(sockfd,5);
        clilen = sizeof(cli_addr);
        newsockfd = accept(sockfd,
            (struct sockaddr *) &cli_addr,
            &clilen);
        SMILE_PRINT("socket accepted\n");
        if (newsockfd < 0)
            error("ERROR on accept");
        while(term=='a'){
            bzero(buffer,256);
            n = read(newsockfd,buffer,255);
            if (n < 0) error("ERROR reading from socket");
            term = buffer[0];
            //printf("Here is the message: %s\n",buffer);
            SMILE_PRINT("msg comes in!\n");
            n = write(newsockfd,"I got your message",18);
            if (n < 0) error("ERROR writing to socket");
        }
        return env->NewStringUTF("just a test");
    }

    JNIEXPORT jstring JNICALL
    Java_com_inMind_inMindAgent_AudioStreamer_vadtestfunc(JNIEnv *env, jobject thiz, jbyteArray jarray, jint array_length)
    {
        jboolean jbool=0;
        jbyte *jb1,*jb2;
        //unsigned char c1,c2;
        jb1=env->GetByteArrayElements(jarray,&jbool);
        //jb2=env->GetByteArrayElements(array,&jbool);
        
        //unsigned short us1= ((*jb1)<<8) | (*jb2);
        //if(us1>64)
        //    return env->NewStringUTF("vad testing");
        //else
        env->ReleaseByteArrayElements(jarray,jb1,0);
        return env->NewStringUTF("distracted maybe XD");
    }

    JNIEXPORT jstring JNICALL
    Java_com_example_tingyao_voicetest_AudioStreamer_jniUtilTest(JNIEnv *env, jobject thiz, jobjectArray myArray)
    {

        int dim1 = env -> GetArrayLength(myArray);
        jshortArray shortbuf=(jshortArray)env->GetObjectArrayElement(myArray, 0);
        int dim2 = env -> GetArrayLength(shortbuf);
        jshort *jtmp;
        float **data=new float*[dim1];
        
        for(int i=0;i<dim1;i++){
            shortbuf = (jshortArray)env->GetObjectArrayElement(myArray, i);
            jtmp = env->GetShortArrayElements(shortbuf, 0);
            data[i]=new float[dim2];
            for(int j=0;j<dim2;j++){
                data[i][j]=(float)jtmp[j];
            }
            printf("data: %f\n",data[i][0]);
            env->ReleaseShortArrayElements(shortbuf,jtmp,0);
        }
        //env->ReleaseObjectArrayElements(myarray,shortbuf,0);
        return env->NewStringUTF("just a test");
    }

    //JNIEXPORT jstring JNICALL
    //Java_com_inMind_inMindAgent_AudioStreamer_opensmilefunc(JNIEnv *env, jobject thiz)
    JNIEXPORT jfloatArray JNICALL
    Java_com_example_tingyao_voicetest_AudioStreamer_opensmilefunc(JNIEnv *env, jobject thiz, jobjectArray myArray)
    {
        cComponentManager *cmanGlob = NULL;
        jfloatArray opensmile_output;  //final output of opensmile on android
        try{

            smileCommon_fixLocaleEnUs();
            LOGGER.setLogLevel(1);
            LOGGER.enableConsoleOutput();
            
      
            int argc=3;
            char **argv=new char *[3];
            char c0[]="SMILExtract";
            char c1[]="-C";
            char c2[]="/sdcard/myexample4.conf";
            //char c3[]="-I";
            //char c4[]="/sdcard/opensmile.wav";
            //char c5[]="-O";
            //char c6[]="tmp.fea";
            argv[0]=c0;
            argv[1]=c1;
            argv[2]=c2;
            //argv[3]=c3;
            //argv[4]=c4;
            //argv[5]=c5;
            //argv[6]=c6;
        
            cCommandlineParser cmdline(argc,argv);
            
            cmdline.addStr( "configfile", 'C', "Path to openSMILE config file", "smile.conf" );
            cmdline.addInt( "loglevel", 'l', "Verbosity level (0-9)", 2 );
            cmdline.addInt( "nticks", 't', "Number of ticks to process (-1 = infinite) (only works for single thread processing, i.e. nThreads=1)", -1 );
            //cmdline.addBoolean( "configHelp", 'H', "Show documentation of registered config types (on/off)", 0 );
            cmdline.addBoolean( "components", 'L', "Show component list", 0 );
            cmdline.addStr( "configHelp", 'H', "Show documentation of registered config types (on/off/argument) (if an argument is given, show only documentation for config types beginning with the name given in the argument)", NULL, 0 );
            cmdline.addStr( "configDflt", 0, "Show default config section templates for each config type (on/off/argument) (if an argument is given, show only documentation for config types beginning with the name given in the argument, OR for a list of components in conjunctions with the 'cfgFileTemplate' option enabled)", NULL, 0 );
            cmdline.addBoolean( "cfgFileTemplate", 0, "Print a complete template config file for a configuration containing the components specified in a comma separated string as argument to the 'configDflt' option", 0 );
            cmdline.addBoolean( "cfgFileDescriptions", 0, "Include description in config file templates.", 0 );
            cmdline.addBoolean( "ccmdHelp", 'c', "Show custom commandline option help (those specified in config file)", 0 );
            cmdline.addStr( "logfile", 0, "set log file", "smile.log" );
            cmdline.addBoolean( "nologfile", 0, "don't write to a log file (e.g. on a read-only filesystem)", 0 );
            cmdline.addBoolean( "noconsoleoutput", 0, "don't output any messages to the console (log file is not affected by this option)", 0 );
            cmdline.addBoolean( "appendLogfile", 0, "append log messages to an existing logfile instead of overwriting the logfile at every start", 0 );
            

            int help = 0;
            if (cmdline.doParse() == -1) {
                LOGGER.setLogLevel(0);
                help = 1;
            }

            if (argc <= 1) {
                printf("\nNo commandline options were given.\n Please run ' SMILExtract -h ' to see some usage information!\n\n");
                //return env->NewStringUTF("opensmile return 10");
            }

            //if (help==1) { return env->NewStringUTF("opensmile return help=1"); };
/*
//LOGGER will lead opensmile to crash
            if (cmdline.getBoolean("nologfile")) {
                LOGGER.setLogFile((const char *)NULL,0,!(cmdline.getBoolean("noconsoleoutput")));
            } else {
            LOGGER.setLogFile(cmdline.getStr("logfile"),cmdline.getBoolean("appendLogfile"),!(cmdline.getBoolean("noconsoleoutput")));
            }
            LOGGER.setLogLevel(cmdline.getInt("loglevel"));
            //SMILE_MSG(2,"openSMILE starting!");
*/          

            //SMILE_MSG(2,"config file is: %s",cmdline.getStr("configfile"));
            // create configManager:
            cConfigManager *configManager = new cConfigManager(&cmdline);
        SMILE_PRINT("here\n");
            cComponentManager *cMan = new cComponentManager(configManager,componentlist);

        SMILE_PRINT("here1\n");
            const char *selStr=NULL;
            if (cmdline.isSet("configHelp")) {
#ifndef EXTERNAL_BUILD
                selStr = cmdline.getStr("configHelp");
                configManager->printTypeHelp(1,selStr,0);
#endif
                help = 1;
            }
            if (cmdline.isSet("configDflt")) {
#ifndef EXTERNAL_BUILD
                int fullMode=0; 
                int wDescr = 0;
                if (cmdline.getBoolean("cfgFileTemplate")) fullMode=1;
                if (cmdline.getBoolean("cfgFileDescriptions")) wDescr=1;
                selStr = cmdline.getStr("configDflt");
                configManager->printTypeDfltConfig(selStr,1,fullMode,wDescr);
#endif
                help = 1;
            }
            if (cmdline.getBoolean("components")) {
#ifndef EXTERNAL_BUILD
                cMan->printComponentList();
#endif  // EXTERNAL_BUILD
                help = 1;
            }

            /*
            if (help==1) {
                delete configManager;
                delete cMan;
                return env->NewStringUTF("opensmile return -1");
            }*/


            SMILE_PRINT("here2\n");
            // TODO: read config here and print ccmdHelp...
            // add the file config reader:
            cFileConfigReader *cFCR=new cFileConfigReader( cmdline.getStr("configfile"), -1, &cmdline);

            try{ 
                //configManager->addReader( new cFileConfigReader( cmdline.getStr("configfile"), -1, &cmdline) );
                configManager->addReader( cFCR );
                SMILE_PRINT("here3\n");
                configManager->readConfig();
                SMILE_PRINT("here4\n");
            } catch (cConfigException *cc) {
                //return env->NewStringUTF("opensmile return 0");
            }

            /* re-parse the command-line to include options created in the config file */

            cmdline.doParse(1,0); // warn if unknown options are detected on the commandline
            if (cmdline.getBoolean("ccmdHelp")) {
                cmdline.showUsage();
                delete configManager;
                delete cMan;
                //return env->NewStringUTF("opensmile return -1");
            }
            
            //fake a input
            /*
            int length=32000;
            float *farray=new float[length];
            for (int i=0;i<length;i++)
                farray[i]=0;
            */
            //now it's true input
            int dim1 = env -> GetArrayLength(myArray);
            jshortArray shortbuf=(jshortArray)env->GetObjectArrayElement(myArray, 0);
            int dim2 = env -> GetArrayLength(shortbuf);
            jshort *jtmp;
            int length=dim1*dim2;
            float *data=new float[dim1*dim2];
        
            for(int i=0;i<dim1;i++){
                shortbuf = (jshortArray)env->GetObjectArrayElement(myArray, i);
                jtmp = env->GetShortArrayElements(shortbuf, 0);
                for(int j=0;j<dim2;j++){
                    data[i*dim2+j]=(float)jtmp[j];
                }
                //printf("data: %f\n",data[i][0]);
                env->ReleaseShortArrayElements(shortbuf,jtmp,0);
            }
            

            configManager->setptrDouble(data);
            configManager->setStreamLength(length);

            //initial output
            int outputln=39*100;
            int outputuseln=0;
            float *outputdata=new float[outputln];
            configManager->initialOutput(outputln);
            


            /* create all instances specified in the config file */
            cMan->createInstances(0); // 0 = do not read config (we already did that above..)

    /*
    MAIN TICK LOOP :
    */

            //cmanGlob = cMan;
            //signal(SIGINT, INThandler); // install Ctrl+C signal handler

            /* run single or mutli-threaded, depending on componentManager config in config file */
            //long long nTicks = cMan->runMultiThreaded(cmdline.getInt("nticks"));

            //before delete config, we need output
            outputuseln=configManager->getCurrentPos();
            jfloat *tmpoutput=configManager->getOutputPtr();
            opensmile_output=env -> NewFloatArray(outputuseln);
            env->SetFloatArrayRegion(opensmile_output, 0, outputuseln, tmpoutput);


            /* it is important that configManager is deleted BEFORE componentManger! 
            (since component Manger unregisters plugin Dlls, which might have allocated configTypes, etc.) */
            delete configManager;
            delete cMan;


            } catch(cSMILException *c) {
            }
        //return env->NewStringUTF("opensmile done!!");
        return opensmile_output;
    }
}
