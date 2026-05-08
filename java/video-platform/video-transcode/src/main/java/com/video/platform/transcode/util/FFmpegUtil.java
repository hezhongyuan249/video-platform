package com.video.platform.transcode.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FFmpegUtil {

    private static final Pattern DURATION_PATTERN = Pattern.compile("Duration: (\\d+):(\\d+):(\\d+)");
    private static final Pattern VIDEO_DURATION_PATTERN = Pattern.compile("time=(\\d+):(\\d+):(\\d+)");

    private static String FFPROBE_PATH = null;
    private static String FFMPEG_PATH = null;

    static {
        initFFmpegPath();
    }

    private static void initFFmpegPath() {
        String[] possiblePaths = {
            "C:/ffmpeg-8.1-full_build/bin/ffprobe.exe",
            "C:/ffmpeg-8.1-full_build/bin/ffmpeg.exe",
            "c:/ffmpeg-8.1-full_build/bin/ffprobe.exe",
            "c:/ffmpeg-8.1-full_build/bin/ffmpeg.exe",
            "C:/ffmpeg/bin/ffprobe.exe",
            "C:/ffmpeg/bin/ffmpeg.exe",
            "C:/Program Files/ffmpeg/bin/ffprobe.exe",
            "C:/Program Files/ffmpeg/bin/ffmpeg.exe",
            "ffprobe",
            "ffmpeg"
        };

        for (String path : possiblePaths) {
            if (testCommand(path)) {
                if (path.contains("ffprobe")) {
                    FFPROBE_PATH = path;
                } else if (path.contains("ffmpeg")) {
                    FFMPEG_PATH = path;
                }
            }
        }

        if (FFPROBE_PATH == null) FFPROBE_PATH = "ffprobe";
        if (FFMPEG_PATH == null) FFMPEG_PATH = "ffmpeg";
    }

    private static boolean testCommand(String command) {
        try {
            String cmd = command.replace(".exe", "");
            ProcessBuilder pb = new ProcessBuilder(cmd, "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public Integer getVideoDuration(String videoPath) {
        if (!StringUtils.hasText(videoPath)) {
            return 0;
        }

        try {
            String[] command = {FFPROBE_PATH, "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", videoPath};
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            String result = output.toString().trim();

            if (exitCode != 0) {
                return getVideoDurationFallback(videoPath);
            }

            if (result.isEmpty()) {
                return getVideoDurationFallback(videoPath);
            }

            try {
                double durationSeconds = Double.parseDouble(result);
                int duration = (int) Math.round(durationSeconds);
                return duration;
            } catch (NumberFormatException e) {
                return getVideoDurationFallback(videoPath);
            }

        } catch (Exception e) {
            return getVideoDurationFallback(videoPath);
        }
    }

    private Integer getVideoDurationFallback(String videoPath) {
        try {
            String[] command = {FFMPEG_PATH, "-i", videoPath};
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            process.waitFor();

            String result = output.toString();
            Matcher matcher = DURATION_PATTERN.matcher(result);

            if (matcher.find()) {
                int hours = Integer.parseInt(matcher.group(1));
                int minutes = Integer.parseInt(matcher.group(2));
                int seconds = Integer.parseInt(matcher.group(3));
                int duration = hours * 3600 + minutes * 60 + seconds;
                return duration;
            }

        } catch (Exception e) {
        }
        return 0;
    }

    public boolean transcodeVideo(String inputPath, String outputPath, String resolution, TranscodeProgress progress, String resolutionType) {
        if (!StringUtils.hasText(inputPath) || !StringUtils.hasText(outputPath) || !StringUtils.hasText(resolution)) {
            return false;
        }

        String scale;
        switch (resolution) {
            case "1080p":
                scale = "1920:1080";
                break;
            case "720p":
                scale = "1280:720";
                break;
            case "480p":
                scale = "854:480";
                break;
            default:
                return false;
        }

        try {
            Integer totalDuration = getVideoDuration(inputPath);
            if (totalDuration == null || totalDuration <= 0) {
                totalDuration = 1;
            }
            
            String[] command = {FFMPEG_PATH, "-i", inputPath, "-vf", "scale=" + scale, "-c:v", "libx264", "-preset", "fast", "-crf", "23", "-c:a", "aac", "-b:a", "128k", "-y", outputPath};
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 使用单独线程读取FFmpeg输出
            final Integer finalDuration = totalDuration;
            Thread readerThread = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (progress != null && line.contains("time=")) {
                            try {
                                int currentTime = parseTime(line);
                                if (currentTime > 0 && finalDuration > 0) {
                                    int percent = (int) ((currentTime * 100.0) / finalDuration);
                                    percent = Math.min(percent, 99);
                                    if ("480p".equals(resolutionType)) {
                                        progress.setProgress480p(percent);
                                    } else if ("720p".equals(resolutionType)) {
                                        progress.setProgress720p(percent);
                                    } else if ("1080p".equals(resolutionType)) {
                                        progress.setProgress1080p(percent);
                                    }
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                    reader.close();
                } catch (Exception e) {
                }
            });
            readerThread.start();

            // 等待FFmpeg进程完成
            int exitCode = process.waitFor();
            
            // 等待读取线程结束
            readerThread.join(3000);
            
            if (progress != null) {
                if ("480p".equals(resolutionType)) {
                    progress.setProgress480p(100);
                } else if ("720p".equals(resolutionType)) {
                    progress.setProgress720p(100);
                } else if ("1080p".equals(resolutionType)) {
                    progress.setProgress1080p(100);
                }
            }
            return exitCode == 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private int parseTime(String line) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("time=(\\d+):(\\d+):(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                int hours = Integer.parseInt(matcher.group(1));
                int minutes = Integer.parseInt(matcher.group(2));
                int seconds = Integer.parseInt(matcher.group(3));
                return hours * 3600 + minutes * 60 + seconds;
            }
        } catch (Exception e) {
        }
        return 0;
    }
    
    public boolean transcodeVideo(String inputPath, String outputPath, String resolution) {
        return transcodeVideo(inputPath, outputPath, resolution, null, null);
    }
}
