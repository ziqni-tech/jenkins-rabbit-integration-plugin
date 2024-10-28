package com.ziqni.jenkins.plugins.rabbit.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.UUID;

public abstract class MachineIdentifier {
    public static final String HEADER_MACHINE_ID = "machine-id";

    private static String id;

    public static String getUniqueMachineId() {

        if(id != null) {
            return id;
        }

        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                id = getWindowsMachineId();
                return id;
            } else if (os.contains("mac")) {
                id = getMacSerialNumber();
                return id;
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                id = getLinuxMachineId();
                return id;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback to MAC Address or UUID generation
        String macAddress = getMacAddress();
        if (!macAddress.equals("UNKNOWN")) {
            id = macAddress;
            return id;
        }
        id = UUID.randomUUID().toString();
        return id;
    }

    // Windows: Get machine UUID using wmic command
    private static String getWindowsMachineId() throws Exception {
        ProcessBuilder builder = new ProcessBuilder("wmic", "csproduct", "get", "UUID");
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.readLine(); // Skip the header line
            return reader.readLine().trim();
        }
    }

    // macOS: Get the system serial number using ioreg command
    private static String getMacSerialNumber() throws Exception {
        ProcessBuilder builder = new ProcessBuilder("ioreg", "-l");
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("IOPlatformSerialNumber")) {
                    return line.split("=")[1].replace("\"", "").trim();
                }
            }
        }
        return "UNKNOWN";
    }

    // Linux: Get machine ID from /etc/machine-id
    private static String getLinuxMachineId() throws Exception {
        ProcessBuilder builder = new ProcessBuilder("cat", "/etc/machine-id");
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.readLine().trim();
        }
    }

    // Get MAC Address as a fallback method
    private static String getMacAddress() {
        try {
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                byte[] mac = network.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "UNKNOWN";
    }

    public static void main(String[] args) {
        System.out.println("Unique Machine ID: " + getUniqueMachineId());
    }
}
