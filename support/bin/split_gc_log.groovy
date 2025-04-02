#!/usr/bin/env groovy

@Grab(group = 'commons-io', module = 'commons-io', version = '2.0.1') import org.apache.commons.io.FileUtils

import java.util.zip.GZIPOutputStream

user = "test";
server = "server_achieve";

String fileName = args.length > 0 ? args[0] : "gc.log";

File logsDir = new File("/home/${user}/${server}/logs/");
File destDir = new File(logsDir, "gc");
if(!destDir.exists()) {
    destDir.mkdir();
}

splitGcLogFile(new File(logsDir, fileName), destDir)

def splitGcLogFile(File inFile, File destDir) {
    List<String> strings = FileUtils.readLines(inFile);
    List<String> out = new ArrayList<String>();
    String currDate = null;
    String startTime = null;
    String lastTime = null;
    //2012-04-03T04:34:37.450+0400: 5.372: [GC 5.372: [ParNew: 838912K->34080K(943744K), 0.0293730 secs] 838912K->34080K(3040896K), 0.0294600 secs] [Times: user=0.22 sys=0.02, real=0.03 secs]
    for(String line : strings) {
        if(startWithDate(line)) {
            String date = line.substring(0, 10);
            String time = line.substring(11, 16);
            if(currDate == null) {
                currDate = date;
                startTime = time;
            }
            if(!currDate.equals(date)) {
                writeFile(destDir, out, currDate, startTime, lastTime);
                out.clear();
                currDate = date;
                startTime = time;
            }

            out.add(line.substring(30));
            lastTime = time;
        } else {
            out.add(line);
        }
    }

    writeFile(destDir, out, currDate, startTime, lastTime);
}

def boolean startWithDate(String line) {
    return Character.isDigit(line.charAt(0)) &&
            Character.isDigit(line.charAt(1)) &&
            Character.isDigit(line.charAt(2)) &&
            Character.isDigit(line.charAt(3)) &&
            line.charAt(4) == '-'
    ;
}

def void writeFile(File root, List<String> out, String currDate, String startTime, String lastTime) throws IOException {
    File zipped = new File(root, String.format("gc_%s_%s-%s.log.gz", currDate, startTime.replaceAll(":", ""), lastTime.replaceAll(":", "")));
    if(zipped.exists()) {
        System.out.println("file exists " + zipped);
    } else {
        System.out.println("write " + zipped + " ...");
        BufferedOutputStream outStream = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(zipped)));
        for(String line : out) {
            outStream.write("${line}\n".getBytes());
        }
        outStream.close();
    }

}