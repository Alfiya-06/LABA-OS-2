import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;

public class MatrixMul {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Использование: java MatrixMul <N> <P>");
            System.err.println("  N — размер матрицы (целое положительное число)");
            System.err.println("  P — число процессов (целое число, не менее 1)");
            System.exit(1);
        }

        int N, P;
        try {
            N = Integer.parseInt(args[0]);
            P = Integer.parseInt(args[1]);

            // Проверки — внутри try, где N и P точно инициализированы
            if (N <= 0) {
                System.err.println("Ошибка: N должно быть положительным");
                System.exit(1);
            }
            if (P <= 0) {
                System.err.println("Ошибка: P должно быть не меньше 1");
                System.exit(1);
            }
            if (P > N) {
                P = N;
            }

        } catch (NumberFormatException e) {
            System.err.println("Ошибка: N и P должны быть целыми числами");
            System.exit(1);
            return; 
        }

        // Теперь N и P точно инициализированы — компилятор доволен
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
                            "java", "Worker",
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

            Files.deleteIfExists(Paths.get("A.bin"));
            Files.deleteIfExists(Paths.get("B.bin"));
            Files.deleteIfExists(Paths.get("C.bin"));

        } catch (Exception e) {
            System.err.println("Ошибка во время выполнения: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
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
