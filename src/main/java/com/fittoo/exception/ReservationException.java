package com.fittoo.exception;

import com.fittoo.common.message.ReservationErrorMessage;
import lombok.Getter;

@Getter
public class ReservationException extends RuntimeException{

	private String memberId;
	private Long reservationId;
	public ReservationException() {
		super();
	}

	public ReservationException(String message) {
		super(message);
	}

	public ReservationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReservationException(Throwable cause) {
		super(cause);
	}

	protected ReservationException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ReservationException(String message, String memberId, Long reservationId) {
		super(message);
		this.memberId = memberId;
		this.reservationId = reservationId;
	}
}
