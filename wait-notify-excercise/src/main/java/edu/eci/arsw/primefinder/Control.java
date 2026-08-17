package edu.eci.arsw.primefinder;

import java.util.Scanner;

/**
 * Hilo supervisor. Arranca los trabajadores y luego, cada TMILISECONDS,
 * pausa a todos, reporta cuantos primos se llevan y espera ENTER para
 * reanudar.
 */
public class Control extends Thread {

    private static final int NTHREADS = 3;
    private static final int MAXVALUE = 30000000;
    private static final int TMILISECONDS = 5000;

    private final int NDATA = MAXVALUE / NTHREADS;

    private final PrimeFinderThread[] pft;

    private final PauseControl pauseControl = new PauseControl();

    private Control() {
        super();
        this.pft = new PrimeFinderThread[NTHREADS];

        int i;
        for (i = 0; i < NTHREADS - 1; i++) {
            pft[i] = new PrimeFinderThread(i * NDATA, (i + 1) * NDATA, pauseControl);
        }
        pft[i] = new PrimeFinderThread(i * NDATA, MAXVALUE + 1, pauseControl);
    }

    public static Control newControl() {
        return new Control();
    }

    @Override
    public void run() {
        for (int i = 0; i < NTHREADS; i++) {
            pft[i].start();
        }

        Scanner consola = new Scanner(System.in);

        try {
            while (workersActivos()) {
                Thread.sleep(TMILISECONDS);

                pauseControl.pause();

                System.out.println();
                System.out.println("=== PAUSA ===");
                int total = 0;
                for (int i = 0; i < NTHREADS; i++) {
                    int encontrados = pft[i].getPrimes().size();
                    System.out.printf("  Hilo %d: %d primos%n", i, encontrados);
                    total += encontrados;
                }
                System.out.println("  TOTAL: " + total + " primos");
                System.out.println("Presione ENTER para reanudar...");

                if (consola.hasNextLine()) {
                    consola.nextLine();
                }

                pauseControl.resume();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Busqueda terminada.");
    }

    private boolean workersActivos() {
        for (PrimeFinderThread t : pft) {
            if (t.isAlive()) {
                return true;
            }
        }
        return false;
    }

}
