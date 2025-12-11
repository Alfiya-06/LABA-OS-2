import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;

public class MatrixMul {
    public static void main(String[] args) {
        int N = 800;

        // Массив чисел процессов для тестирования
        int[] P_values = {1, 2, 4, 8};

        for (int P : P_values) {
            if (P > N) {
                System.err.println("Предупреждение: P=" + P + " > N=" + N + " — ограничено до N");
                P = N;
            }

            try {
                long totalSize = (long) N * N * 8;
                Random rnd = new Random(42);

                createMatrix("A.bin", N, rnd);
                createMatrix("B.bin", N, rnd);

                final int RUNS = 3;
                long totalTime = 0;

                for (int run = 0; run < RUNS; run++) {
                    Files.deleteIfExists(Paths.get("C.bin"));

                    try (FileChannel ch = FileChannel.open(Paths.get("C.bin"),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.READ,
                            StandardOpenOption.WRITE)) {
                        ch.truncate(totalSize);
                    }

                    long start = System.nanoTime();

                    int rowsPer = (N + P - 1) / P;
                    Process[] workers = new Process[P];

                    for (int p = 0; p < P; p++) {
                        int startRow = p * rowsPer;
                        int endRow = Math.min(startRow + rowsPer, N);

                        ProcessBuilder pb = new ProcessBuilder(
                                "java", "-cp", System.getProperty("java.class.path"),
                                "Worker",
                                String.valueOf(N),
                                String.valueOf(startRow),
                                String.valueOf(endRow)
                        );
                        workers[p] = pb.start();
                    }

                    for (Process pr : workers) {
                        pr.waitFor();
                    }

                    long end = System.nanoTime();
                    totalTime += (end - start);
                }

                double avgMs = totalTime / (RUNS * 1_000_000.0);
                System.out.printf("N=%d P=%d avg_time=%.2f ms%n", N, P, avgMs);

                // Очистка временных файлов после каждого P
                Files.deleteIfExists(Paths.get("A.bin"));
                Files.deleteIfExists(Paths.get("B.bin"));
                Files.deleteIfExists(Paths.get("C.bin"));

            } catch (Exception e) {
                System.err.println("Ошибка при P=" + P + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    static void createMatrix(String name, int N, Random rnd) throws Exception {
        long size = (long) N * N * 8;
        try (FileChannel ch = FileChannel.open(Paths.get(name),
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {
            ch.truncate(size);
            MappedByteBuffer buf = ch.map(FileChannel.MapMode.READ_WRITE, 0, size);
            for (int i = 0; i < N * N; i++) {
                buf.putDouble(rnd.nextDouble());
            }
        }
    }
}