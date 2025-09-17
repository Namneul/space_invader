package org.newdawn.spaceinvaders;

public final class SystemTimer {
	/** 고해상도 타이머의 기준 시점(클래스 로딩 순간) */
	private static final long START_NANOS = System.nanoTime();
	/** "타이머 틱/초" 개념을 유지하려면 나노초를 틱으로 간주 (1초 = 1_000_000_000 틱) */
	@SuppressWarnings("unused")
	private static final long TIMER_TICKS_PER_SECOND = 1_000_000_000L;

	private SystemTimer() { /* 유틸 클래스 */ }

	/**
	 * 고해상도 시간(ms)을 반환 (클래스 초기화 이후 경과 시간)
	 * @return 밀리초 단위의 경과 시간
	 */
	public static long getTime() {
		// nanoTime은 단조 증가(monotonic)하므로 경과 시간 측정에 적합
		return (System.nanoTime() - START_NANOS) / 1_000_000L;
	}

	/**
	 * 지정 ms 동안 대기
	 * @param duration 대기 시간(ms)
	 */
	public static void sleep(long duration) {
		if (duration <= 0) return;

		final long deadline = System.nanoTime() + duration * 1_000_000L;
		while (true) {
			long remaining = deadline - System.nanoTime();
			if (remaining <= 0) break;

			long millis = remaining / 1_000_000L;
			int nanos = (int) (remaining - millis * 1_000_000L);
			try {
				// 남은 시간이 1ms 이상이면 ms+nanos로, 아주 짧으면 0ms + nanos로 잠깐 쉼
				Thread.sleep(millis, nanos);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}
}