package edu.eci.arsw.primefinder;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PrimeFinderThread extends Thread {

	int a, b;

	private final PauseControl pauseControl;

	private final List<Integer> primes;

	public PrimeFinderThread(int a, int b, PauseControl pauseControl) {
		super();
		this.primes = Collections.synchronizedList(new LinkedList<>());
		this.a = a;
		this.b = b;
		this.pauseControl = pauseControl;
	}

	@Override
	public void run() {
		try {
			for (int i = a; i < b; i++) {
				pauseControl.awaitIfPaused();
				if (isPrime(i)) {
					primes.add(i);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	boolean isPrime(int n) {
		boolean ans;
		if (n > 2) {
			ans = n % 2 != 0;
			for (int i = 3; ans && i * i <= n; i += 2) {
				ans = n % i != 0;
			}
		} else {
			ans = n == 2;
		}
		return ans;
	}

	public List<Integer> getPrimes() {
		return primes;
	}

}
