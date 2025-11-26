package org.newdawn.spaceinvaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalServerManager {

    private static final Logger logger = Logger.getLogger(LocalServerManager.class.getName());
    private final String classpath;

    public LocalServerManager() {
        this.classpath = System.getProperty("java.class.path");
    }

    /**
     * 서버 프로세스를 시작합니다.
     * @param port 포트 번호
     * @param serverType "single" 또는 null
     * @param logPrefix 로그에 표시할 이름 (예: [Local Server])
     * @return 실행된 Process 객체
     */
    public Process startServerProcess(int port, String serverType, String logPrefix) throws IOException {
        ProcessBuilder pb;
        if ("single".equals(serverType)) {
            pb = new ProcessBuilder("java", "-Dfile.encoding=UTF-8", "-cp", classpath,
                    "org.newdawn.spaceinvaders.multiplay.Server", String.valueOf(port), "single");
        } else {
            pb = new ProcessBuilder("java", "-Dfile.encoding=UTF-8", "-cp", classpath,
                    "org.newdawn.spaceinvaders.multiplay.Server", String.valueOf(port));
        }

        pb.redirectErrorStream(true);
        pb.directory(new File(".")); // 실행 위치 설정

        Process process = pb.start();
        logger.log(Level.INFO, "{0} 시작됨 (포트: {1})", new Object[]{logPrefix, port});

        // 로그 읽기 스레드 시작
        startProcessLogReader(process, logPrefix);

        return process;
    }

    private void startProcessLogReader(Process process, String logPrefix) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.log(Level.INFO, "{0}: {1}", new Object[]{logPrefix, line});
                }
            } catch (IOException e) {
                // 프로세스 종료 시 발생하는 예외는 무시
            }
        }, logPrefix + "-logger").start();
    }

    public void cleanupServerProcess(Process process) {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            logger.info("서버 프로세스 종료됨.");
        }
    }
}