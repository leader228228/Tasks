package ua.sumdu.j2se.Birintsev.tasks;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskIO {

    public final static String dateFormate = "yyyy-MM-dd HH:mm:ss.SSS";

    public static void write(TaskList tasks, OutputStream out) throws IOException {
        DataOutputStream dataOutputStream = null;
        try{
            dataOutputStream = new DataOutputStream(out);
            dataOutputStream.writeInt(tasks.size());
            for(Task task : tasks){
                dataOutputStream.writeInt(task.getTitle().length());
                for(int i = 0; i < task.getTitle().length(); i++){
                    dataOutputStream.writeChar(task.getTitle().charAt(i));
                }
                dataOutputStream.writeBoolean(task.isActive());
                dataOutputStream.writeInt(task.getRepeatInterval());
                if(task.isRepeated()){
                    dataOutputStream.writeLong(task.getStartTime().getTime());
                    dataOutputStream.writeLong(task.getEndTime().getTime());
                }else{
                    dataOutputStream.writeLong(task.getTime().getTime());
                }
            }
        }finally {
            if(dataOutputStream != null){
                dataOutputStream.close();
            }
        }
    }

    public static void read(TaskList tasks, InputStream in) throws IOException {
        if (in.available() == 0) {
            return; // ??? logger/exception?
        }
        DataInputStream dataInputStream = null;
        try {
            dataInputStream = new DataInputStream(in);
            int amOfTasks = dataInputStream.readInt();
            for (int i = 0; i < amOfTasks; i++) {
                int detailsLength = dataInputStream.readInt();
                StringBuilder taskDetails = new StringBuilder();
                for (int j = 0; j < detailsLength; i++) {
                    taskDetails.append(dataInputStream.readChar());
                }
                boolean isActive = dataInputStream.readBoolean();
                int interval = dataInputStream.readInt();
                if (interval != 0) {
                    long startTime = dataInputStream.readLong();
                    long endTime = dataInputStream.readLong();
                    Task task = new Task(taskDetails.toString(), new Date(startTime), new Date(endTime), interval);
                    task.setActive(isActive);
                    tasks.add(task);
                } else {
                    long time = dataInputStream.readLong();
                    Task task = new Task(taskDetails.toString(), new Date(time));
                    task.setActive(isActive);
                    tasks.add(task);
                }
            }
        } finally {
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            in.close();
        }
    }

    public static void writeBinary(TaskList tasks, File file) throws IOException{
        BufferedOutputStream bufferedOutputStream = null;
        try{
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
            write(tasks,bufferedOutputStream);
        }finally {
            if(bufferedOutputStream != null){
                bufferedOutputStream.close();
            }
        }
    }

    public static void readBinary(TaskList tasks, File file) throws IOException{
        if(!file.canRead()){
            return; ///???
        }
        FileInputStream fileInputStream = null;
        try{
            fileInputStream = new FileInputStream(file);
            read(tasks,fileInputStream);
        }finally {
            if(fileInputStream != null){
                fileInputStream.close();
            }
        }
    }

    public static void write(TaskList tasks, Writer out) throws IOException{
        PrintWriter printWriter = null;
        try {
            printWriter = new PrintWriter(out);
            printWriter.write(tasks.toString());
        }finally {
            if(printWriter != null){
                printWriter.close();
            }
        }
    }

    public static void read(TaskList tasks, Reader in) throws IOException, ParseException{
        if(!in.ready()){
            return; // ??? поток не готов быть считан
        }
        BufferedReader bufferedReader = null;
        try{
            bufferedReader = new BufferedReader(in);
            Pattern patternNonRepeated = Pattern.compile("^\"(.*)\" at \\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})](.*)[.;]$");
            Pattern patternRepeated = Pattern.compile("^\"(.*)\" from \\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})] to \\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})] every \\[(.*)](.*)[.;]$");
            SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormate);
            Task task;
            String stringToParse = bufferedReader.readLine();
            while (stringToParse != null){
                Matcher matcher = patternNonRepeated.matcher(stringToParse);
                if (matcher.find()){
                    String details = matcher.group(1).replace("\"\"", "\"");
                    Date time = dateFormat.parse(matcher.group(2));
                    boolean isActive = matcher.group(3).length() == 0;
                    task = new Task(details,time);
                    task.setActive(isActive);
                    tasks.add(task);
                }else{
                    matcher = patternRepeated.matcher(stringToParse);
                    if(matcher.find()) {
                        String details = matcher.group(1).replace("\"\"", "\"");
                        Date start = dateFormat.parse(matcher.group(2));
                        Date end = dateFormat.parse(matcher.group(3));
                        int interval = parseInterval(matcher.group(4));
                        // if seconds == 0 -> logger warning???
                        boolean isActive = matcher.group(5).length() == 0;
                        task = new Task(details, start, end, interval);
                        task.setActive(isActive);
                        tasks.add(task);
                    }else {
                        throw new IOException(new StringBuilder("Wrong stringToParse value : ").append(stringToParse).toString());
                    }
                }
                stringToParse = bufferedReader.readLine();
            }
        }finally {
            if(bufferedReader != null){
                bufferedReader.close();
            }
        }
    }

    private static int parseInterval(String string){
        if (string == null){
            throw new IllegalArgumentException("The string must not be null");
        }
        int seconds = 0;
        Pattern pattern = Pattern.compile("(\\d) day");
        Matcher matcher = pattern.matcher(string);
        if(matcher.find()){
            seconds += Integer.parseInt(matcher.group(1)) * 86400;
        }
        pattern = Pattern.compile("(\\d) hour");
        matcher = pattern.matcher(string);
        if(matcher.find()){
            seconds += Integer.parseInt(matcher.group(1)) * 3600;
        }
        pattern = Pattern.compile("(\\d) minute");
        matcher = pattern.matcher(string);
        if(matcher.find()){
            seconds += Integer.parseInt(matcher.group(1)) * 60;
        }
        pattern = Pattern.compile("(\\d) second");
        matcher = pattern.matcher(string);
        if(matcher.find()){
            seconds += Integer.parseInt(matcher.group(1));
        }
        return seconds;
    }

    public static void writeText(TaskList tasks, File file) throws IOException{
        if(!file.canWrite()){
            return; ///???
        }
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(file));
            write(tasks, bufferedWriter);
        } finally {
            bufferedWriter.close();
        }
    }

    public static void readText(TaskList tasks, File file) throws IOException{
        if(!file.canRead()){
            return; ///???
        }
        BufferedReader bufferedReader =  null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            read(tasks, bufferedReader);
        } catch (ParseException e) {
            throw new IOException(e);
        }finally {
            bufferedReader.close();
        }
    }
}