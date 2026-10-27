package com.smartshipment.shipment.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.repository.ShipmentRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ShipmentQrCodeService {
	private static final int QR_SIZE = 320;

	private final ShipmentRepository shipments;
	private final String publicTrackingBaseUrl;

	public ShipmentQrCodeService(ShipmentRepository shipments,
			@Value("${app.public-tracking-base-url}") String publicTrackingBaseUrl) {
		this.shipments = shipments;
		this.publicTrackingBaseUrl = publicTrackingBaseUrl;
	}

	@Transactional(readOnly = true)
	public byte[] generateForShipment(Long shipmentId) {
		Shipment shipment = shipments.findById(shipmentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));
		String trackingUrl = UriComponentsBuilder.fromUriString(publicTrackingBaseUrl)
				.pathSegment(shipment.getTrackingNumber())
				.build()
				.toUriString();

		Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		hints.put(EncodeHintType.MARGIN, 2);

		try {
			BitMatrix matrix = new QRCodeWriter().encode(trackingUrl, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			MatrixToImageWriter.writeToStream(matrix, "PNG", output);
			return output.toByteArray();
		} catch (WriterException | IOException exception) {
			throw new IllegalStateException("Could not generate shipment QR code", exception);
		}
	}
}
