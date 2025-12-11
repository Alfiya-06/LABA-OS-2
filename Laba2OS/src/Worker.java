import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;

public class Worker {
    public static void main(String[] args) throws Exception {

        int N = Integer.parseInt(args[0]);
        int startRow = Integer.parseInt(args[1]);
        int endRow = Integer.parseInt(args[2]);

        long size = (long) N * N * 8;

        MappedByteBuffer A = FileChannel.open(Paths.get("A.bin"), StandardOpenOption.READ)
                .map(FileChannel.MapMode.READ_ONLY, 0, size);

        MappedByteBuffer B = FileChannel.open(Paths.get("B.bin"), StandardOpenOption.READ)
                .map(FileChannel.MapMode.READ_ONLY, 0, size);

        MappedByteBuffer C = FileChannel.open(Paths.get("C.bin"), StandardOpenOption.READ, StandardOpenOption.WRITE)
                .map(FileChannel.MapMode.READ_WRITE, 0, size);

        DoubleBuffer Ad = A.asDoubleBuffer();
        DoubleBuffer Bd = B.asDoubleBuffer();
        DoubleBuffer Cd = C.asDoubleBuffer();

        double[] rowA = new double[N];
        double[] colB = new double[N];

        for (int i = startRow; i < endRow; i++) {
            Ad.position(i * N);
            Ad.get(rowA, 0, N);

            for (int j = 0; j < N; j++) {
                for (int k = 0; k < N; k++) {
                    colB[k] = Bd.get(k * N + j);
                }

                double sum = 0.0;
                for (int k = 0; k < N; k++) {
                    sum += rowA[k] * colB[k];
                }

                Cd.put(i * N + j, sum);
            }
        }
    }
}