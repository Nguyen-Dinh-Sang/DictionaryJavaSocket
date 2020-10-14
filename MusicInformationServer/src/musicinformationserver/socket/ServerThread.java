package musicinformationserver.socket;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/*
    - khi một client kết nối tới server thì một luông Socket được tạo ra để phụ vụ client đó.
    - luồng có các phương thức start và stop để server quản lý.
    - phần xử lý.
        -- khi client kết nối và theard được tạo ra thì đọc dữ liệu từ file đổ vào 2 hashmap
        -- client gửi 1 dữ liệu.
            --- kiểm tra dữ liệu là dịch, thêm, xóa
            --- nếu là dịch kiểm tra dữ liệu trong 2 hashmap trả về client kết quả hoặc thông báo không có dữ liệu.
            --- nếu là thêm dùng pattern kiểm tra dữ liệu đúng cấu trúc.
                ---- nếu đúng thì kiểm tra dữ liệu nếu chưa có thì thêm vào 2 hashmap và ghi vào file data một dòng mới.
                ---- nếu không thì trả thông báo cho client.
            --- nếu là xóa dùng pattern kiểm tra dữ liệu đúng cấu trúc.
                ---- nếu đúng cấu trúc, kiểm tra dữ liệu có tồn tại hay k nếu có thì xóa trong 2 hashmap và xóa file sau đó ghi hashmap english-vietnamese vào file mới
                ---- nếu không tồn tại thì trả thông báo cho client.
 */
public class ServerThread implements Runnable {
    private Socket socket;

    //id do server cấp cho mỗi client
    private int clientCount;
    private BufferedWriter out;
    private BufferedReader in;
    private Thread worker;
    private AtomicBoolean running = new AtomicBoolean(false);
    private HashMap<String, String> vietNameseEnglish = new HashMap<>();
    private HashMap<String, String> englishVietNamese = new HashMap<>();
    private final String PATH_FILE = "./src/resource/dictionary.txt";
    private final String ADD_PATTERN = "(^ADD)+(;\\D+;){1}+(\\D+)";
    private final String REMOVE_PATTERN = "(^DEL)+(;\\w+){1}";

    public ServerThread(Socket socket, int clientCount) {
        this.socket = socket;
        this.clientCount = clientCount;
        readFile();
    }

    @Override
    public void run() {
        running.set(true);
        if (socket != null) {
            System.err.println("Client " + clientCount + " accepted");

            try {
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
            }

            while (running.get()) {
                String data;
                try {
                    data = in.readLine();
                    if (data != null) {
                        received(data);
                    }
                    System.out.println("Server received " + clientCount + ": " + data);
                } catch (IOException e) {
                    System.err.println(clientCount + " Lost connection");
                    closeServerThread();
                }
            }
        }
    }

    public void closeServerThread() {
        if (running.get()) {
            System.out.println("Closing thread " + clientCount);

            running.set(false);
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
            }
        }
        System.err.println("Thread " + clientCount + " closed");
    }

    public void startServerThread() {
        worker = new Thread(this);
        worker.start();
    }

    private void readFile() {
        try {
            File fileDictionary = new File(PATH_FILE);
            Scanner scanner = new Scanner(fileDictionary);

            while (scanner.hasNextLine()) {
                try {
                    String data = scanner.nextLine();
                    String[] tmp = data.split(";");
                    vietNameseEnglish.put(tmp[1], tmp[0]);
                    englishVietNamese.put(tmp[0], tmp[1]);
                } catch (Exception e) {

                }
            }

            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void received(String data) {
        if (data.startsWith("ADD")) {
            if (Pattern.matches(ADD_PATTERN, data)) {
                addWork(data);
            } else {
                returnResult("Invalid data (ADD;EnglishWork;VietNameseWork)");
            }
        } else if (data.startsWith("DEL")) {
            if (Pattern.matches(REMOVE_PATTERN, data)) {
                System.err.println("1 " + englishVietNamese.get(data.substring(4)));
                removeWork(data);
            } else {
                returnResult("Invalid data (DEL;EnglishWork)");
            }
        } else if (data.equals("bye")) {
            closeServerThread();
        } else {
            translate(data);
        }
    }

    private void translate(String data) {
        if (englishVietNamese.get(data) != null) {
            returnResult(englishVietNamese.get(data));
        } else if (vietNameseEnglish.get(data) != null) {
            returnResult(vietNameseEnglish.get(data));
        } else returnResult("No data");
    }

    private void returnResult(String data) {
        try {
            out.write(data + " " + clientCount);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addWork(String data) {
        String[] tmp = data.split(";");
        if (englishVietNamese.get(tmp[1]) != null) {
            returnResult("Dulicate");
        } else {
            englishVietNamese.put(tmp[1], tmp[2]);
            vietNameseEnglish.put(tmp[2], tmp[1]);
            try {
                File file = new File(PATH_FILE);
                if (!file.exists()) {
                    file.createNewFile();
                }

                FileWriter fileWriter = new FileWriter(file.getAbsoluteFile(), true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.newLine();
                bufferedWriter.write(data.substring(4));

                bufferedWriter.close();
                fileWriter.close();
                returnResult("ADD success");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void removeWork(String data) {
        String work = data.substring(4);
        if (englishVietNamese.get(work) == null) {
            returnResult("No data-" + work + "-" + englishVietNamese.get(work));
        } else {
            String tmp = englishVietNamese.get(work);
            englishVietNamese.remove(work);
            vietNameseEnglish.remove(tmp);

            try {
                File file = new File(PATH_FILE);
                file.delete();
                file.createNewFile();

                FileWriter fileWriter = new FileWriter(file.getAbsoluteFile(), true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

                for (String i : englishVietNamese.keySet()) {
                    bufferedWriter.write(i + ";" + englishVietNamese.get(i));
                    bufferedWriter.newLine();
                }

                bufferedWriter.close();
                fileWriter.close();
                returnResult("DEL success");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}