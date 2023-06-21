package ru.jucr.backupapplicationgui;

import com.jcraft.jsch.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class BackupManager implements Runnable {
    private ConfigurationManager configurationManager;
    private Controller controller;
    private String REMOTE_SERVER_HOST;
    private int REMOTE_SERVER_PORT;
    private String REMOTE_SERVER_USERNAME;
    private String REMOTE_SERVER_PASSWORD;
    private long BACKUP_INTERVAL;
    private String DESTINATION_PATH;
    private String SOURCE_PATH;
    private int MAX_BACKUPS = 5;

    private BackupType backupType;

    public BackupManager(BackupType backupType, ConfigurationManager configurationManager, Controller controller) {
        this.controller = controller;
        this.backupType = backupType;
        this.configurationManager = configurationManager;
        REMOTE_SERVER_HOST = configurationManager.getServerHost();
        REMOTE_SERVER_PORT = configurationManager.getServerPort();
        REMOTE_SERVER_USERNAME = configurationManager.getUsername();
        REMOTE_SERVER_PASSWORD = configurationManager.getServerPassword();
        BACKUP_INTERVAL = configurationManager.getBackupInterval();
        DESTINATION_PATH = configurationManager.getRemotePath();
        SOURCE_PATH = configurationManager.getSourceFolder();
    }

    @Override
    public void run() {
        switch (backupType) {
            case TRACKING:
                performTracking();
                break;
            case BACKUP:
                performBackup();
                break;
            default:
                System.out.println("Invalid backup type");
                break;
        }
    }


    private void performTracking() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                JSch jsch = new JSch();
                Session session = null;
                ChannelSftp channelSftp = null;

                try {
                    session = jsch.getSession(REMOTE_SERVER_USERNAME, REMOTE_SERVER_HOST, REMOTE_SERVER_PORT);
                    session.setPassword(REMOTE_SERVER_PASSWORD);
                    session.setConfig("StrictHostKeyChecking", "no");
                    session.connect();

                    channelSftp = (ChannelSftp) session.openChannel("sftp");
                    channelSftp.connect();

                    File folder = new File(SOURCE_PATH);
                    scanFolder(channelSftp, folder, "");

                } catch (JSchException e) {
                    e.printStackTrace();
                } finally {
                    if (channelSftp != null && channelSftp.isConnected()) {
                        channelSftp.disconnect();
                    }
                    if (session != null && session.isConnected()) {
                        session.disconnect();
                    }
                }
            }
        }, 0, configurationManager.getBackupInterval() * 1000);
        controller.appendResult("Connection successful");
    }

    private void scanFolder(ChannelSftp channelSftp, File folder, String relativePath) {
        if (folder.isDirectory()) {
            String folderPath = folder.getAbsolutePath();
            String remotePath = DESTINATION_PATH + relativePath + folder.getName();

            try {
                if (!isRemoteFolderExists(channelSftp, remotePath)) {
                    channelSftp.mkdir(remotePath);
//                    System.out.println("Папка: " + folderPath + " - Отправлено на удаленный сервер: " + remotePath);
                    controller.appendResult("Папка: " + folderPath + " - Отправлено на удаленный сервер: " + remotePath);
                }
            } catch (SftpException e) {
                controller.appendResult("Error: " + e.getMessage() + "\n");
                e.printStackTrace();
            }

            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        scanFolder(channelSftp, file, relativePath + folder.getName() + "/");
                    } else {
                        String filePath = file.getAbsolutePath();
                        String remoteFilePath = DESTINATION_PATH + relativePath + folder.getName() + "/" + file.getName();

                        try {
                            if (isRemoteFileOutdated(channelSftp, remoteFilePath, file.lastModified())) {
                                try (InputStream inputStream = new FileInputStream(file)) {
                                    channelSftp.put(inputStream, remoteFilePath);
//                                    System.out.println("Файл: " + filePath + " - Отправлено на удаленный сервер: " + remoteFilePath);
                                    controller.appendResult("Файл: " + filePath + " - Отправлено на удаленный сервер: " + remoteFilePath);
                                }
                            }
                        } catch (IOException | SftpException e) {
                            controller.appendResult("Error: " + e.getMessage() + "\n");
                            e.printStackTrace();
                        }
                    }
                }
            }

            try {
                // Check for deleted files on the remote server
                Vector<ChannelSftp.LsEntry> remoteEntries = channelSftp.ls(remotePath);
                for (ChannelSftp.LsEntry remoteEntry : remoteEntries) {
                    String remoteFileName = remoteEntry.getFilename();
                    if (!remoteFileName.equals(".") && !remoteFileName.equals("..")) {
                        String remoteFilePath = remotePath + "/" + remoteFileName;
                        File localFile = new File(folderPath, remoteFileName);
                        if (!localFile.exists()) {
                            deleteRemoteFile(channelSftp, remoteFilePath);
                        }
                    }
                }
            } catch (SftpException e) {
                controller.appendResult("Error: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }
    }

    private boolean isRemoteFolderExists(ChannelSftp channelSftp, String remotePath) throws SftpException {
        try {
            channelSftp.lstat(remotePath);
            return true;
        } catch (SftpException e) {
            return false;
        }
    }

    private boolean isRemoteFileOutdated(ChannelSftp channelSftp, String remoteFilePath, long localLastModified) throws SftpException {
        try {
            SftpATTRS remoteAttrs = channelSftp.stat(remoteFilePath);
            long remoteLastModified = remoteAttrs.getMTime() * 1000L; // Convert seconds to milliseconds
            return localLastModified > remoteLastModified;
        } catch (SftpException e) {
            return true; // If the file doesn't exist on the remote server, consider it outdated and send it
        }
    }

    private void deleteRemoteFile(ChannelSftp channelSftp, String remoteFilePath) throws SftpException {
        SftpATTRS attrs = channelSftp.lstat(remoteFilePath);
        if (attrs.isDir()) {
            // Если путь к удаленному файлу является директорией, рекурсивно удаляем все файлы и поддиректории
            Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(remoteFilePath);
            for (ChannelSftp.LsEntry entry : entries) {
                String fileName = entry.getFilename();
                if (!fileName.equals(".") && !fileName.equals("..")) {
                    String filePath = remoteFilePath + "/" + fileName;
                    deleteRemoteFile(channelSftp, filePath);
                }
            }
            // После удаления всех файлов и поддиректорий, удаляем саму директорию
            channelSftp.rmdir(remoteFilePath);
//            System.out.println("Папка удалена с удаленного сервера: " + remoteFilePath);
            controller.appendResult("Папка удалена с удаленного сервера: " + remoteFilePath);
        } else {
            // Если путь к удаленному файлу является файлом, удаляем файл
            channelSftp.rm(remoteFilePath);
//            System.out.println("Файл удален с удаленного сервера: " + remoteFilePath);
            controller.appendResult("Файл удален с удаленного сервера: " + remoteFilePath);
        }
    }

    private void performBackup() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                // Ваш код, который нужно выполнить
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String backupFolderName = "backup_" + dateFormat.format(new Date());

                String remoteFolderPath = DESTINATION_PATH + "/" + backupFolderName;

                try {
                    // Connect to the remote server via SSH
                    JSch jsch = new JSch();
                    Session session = jsch.getSession(REMOTE_SERVER_USERNAME, REMOTE_SERVER_HOST, REMOTE_SERVER_PORT);
                    session.setPassword(REMOTE_SERVER_PASSWORD);
                    session.setConfig("StrictHostKeyChecking", "no");
                    session.connect();

                    // Create the backup folder remotely
                    Channel channel = session.openChannel("sftp");
                    channel.connect();
                    ChannelSftp sftpChannel = (ChannelSftp) channel;
                    sftpChannel.mkdir(remoteFolderPath);

                    // Backup files from the local folder to the remote folder
                    backupFolder(sftpChannel, SOURCE_PATH, remoteFolderPath);
                    controller.appendResult("Folder " + backupFolderName + " was created\n");

                    // Disconnect from the remote server
                    sftpChannel.exit();
                    session.disconnect();

                    // Remove excess backups if the maximum limit is reached
                    removeExcessBackups();
                } catch (JSchException | SftpException e) {
                    controller.appendResult("Error: " + e.getMessage() + "\n");
                    e.printStackTrace();
                }
            }
        }, 0, configurationManager.getBackupInterval() * 1000);
        controller.appendResult("Connection successful");
    }

    private void backupFolder(ChannelSftp sftpChannel, String localFolderPath, String remoteFolderPath)
            throws SftpException {
        sftpChannel.lcd(localFolderPath);
        sftpChannel.cd(remoteFolderPath);

        File localFolder = new File(localFolderPath);
        File[] files = localFolder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String subLocalFolderPath = localFolderPath + File.separator + file.getName();
                    String subRemoteFolderPath = remoteFolderPath + "/" + file.getName();
                    sftpChannel.mkdir(subRemoteFolderPath);
                    backupFolder(sftpChannel, subLocalFolderPath, subRemoteFolderPath);
                } else {
                    String fileName = file.getName();
                    String remoteFilePath = remoteFolderPath + "/" + fileName;

                    // Проверяем, существует ли файл в текущей папке бэкапа
                    @SuppressWarnings("unchecked")
                    SftpATTRS remoteFile = null;
                    try {
                        remoteFile = sftpChannel.lstat(remoteFilePath);
                    } catch (SftpException e) {
                        // Handle the exception if the file doesn't exist
                        if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                            controller.appendResult("Error: " + e.getMessage() + "\n");
                            e.printStackTrace();
                        }
                    }

                    if (remoteFile == null) {
                        // File is not present in the current backup folder, copy it
                        sftpChannel.put(file.getAbsolutePath(), remoteFilePath);
                    } else {
                        // File already exists in the current backup folder
                        // Check if the file has been modified compared to the previous backup
                        long localFileLastModified = file.lastModified();
                        long remoteFileLastModified = remoteFile.getMTime() * 1000L; // Convert seconds to milliseconds

                        if (localFileLastModified > remoteFileLastModified) {
                            // File has been modified, replace it in the current backup folder
                            sftpChannel.put(file.getAbsolutePath(), remoteFilePath);
                        }
                    }
                }
            }
        }
    }

    private void removeExcessBackups() {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(REMOTE_SERVER_USERNAME, REMOTE_SERVER_HOST, REMOTE_SERVER_PORT);
            session.setPassword(REMOTE_SERVER_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            sftpChannel.cd(DESTINATION_PATH);

            @SuppressWarnings("unchecked")
            java.util.Vector<ChannelSftp.LsEntry> backupFolderList = sftpChannel.ls("backup_*");
            if (backupFolderList.size() > MAX_BACKUPS) {
                backupFolderList.sort(Comparator.comparing(ChannelSftp.LsEntry::getFilename));
                int foldersToDelete = backupFolderList.size() - MAX_BACKUPS;
                for (int i = 0; i < foldersToDelete; i++) {
                    ChannelSftp.LsEntry entry = backupFolderList.get(i);
                    String folderPath = DESTINATION_PATH + entry.getFilename();
                    deleteFolder(sftpChannel, folderPath);
                }
            }

            sftpChannel.exit();
            session.disconnect();
        } catch (JSchException | SftpException e) {
            controller.appendResult("Error: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    private static void deleteFolder(ChannelSftp sftpChannel, String folderPath) throws SftpException {
        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> fileAndFolderList = sftpChannel.ls(folderPath);
        for (ChannelSftp.LsEntry item : fileAndFolderList) {
            if (!item.getAttrs().isDir()) {
                // Удалить файл
                sftpChannel.rm(folderPath + "/" + item.getFilename());
            } else if (!".".equals(item.getFilename()) && !"..".equals(item.getFilename())) {
                // Рекурсивно удалить вложенную папку
                deleteFolder(sftpChannel, folderPath + "/" + item.getFilename());
            }
        }

        // Удалить саму папку
        sftpChannel.rmdir(folderPath);
    }
    public enum BackupType {
        BACKUP, TRACKING
    }
}