package InMind;

/**
 * Created by Amos Azaria on 24-Dec-14.
 */
public class Consts {
    public static final int serverPort = 4493;
    static public final int sampleRate = 16000;//44100;//16000;

    static public final String commandChar = "^";
    static public final String messageSeparatorForPattern = "\\^";
    static public final String serverMessagePattern = "(\\p{Alpha}*)" + Consts.messageSeparatorForPattern + "(.*)";

    //connection commands from server
    static public final String connectUdp = "ConnectUDP";
    static public final String stopUdp = "StopUDP";
    static public final String sayCommand = "Say";
    static public final String launchCommand = "Launch";
    static public final String resumeCommand = "Resume";
    static public final String playbackCommand = "PlayBack";
    static public final String startNewConnection = "StartNewConnection";
    static public final String closeConnection = "CloseConnection";

    //client requests must start with user Id. e.g., userId^SendingText^text
    static public final String clientMessagePattern = "[-a-zA-Z0-9]+" + Consts.messageSeparatorForPattern + "(\\p{Alpha}*)" + Consts.messageSeparatorForPattern + "(.*)";
    //client requests
    static public final String requestSendAudio = "RequestSendAudio";
    static public final String sendingText = "SendingText";
    static public final String closeSession = "closeSession";
    static public final String initSession = "initSession";
    static public final String tcpPing = "tcpPing";
    static public final String distractionFound = "distractionFound";
    static public final String distractionResumed = "distractionResumed";
    static public final String userPlaybackDone = "UserPlaybackDone";
}

