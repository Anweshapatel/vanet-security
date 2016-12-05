package globals;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;

/**
 * Extension of the BeaconDTO but with signature
 */
public class SignedBeaconDTO extends SignedDTO {
    public static final long serialVersionUID = 0;
	private BeaconDTO beaconDTO;

	/**
	 * Builds a signed BeaconDTO
	 * @param	Vector2D			position vector
	 * @param	Vector2D			velocity vector
	 * @param	Timestamp			timestamp of this beacon
	 * @param	X509Certificate		certificate the entity sending this beacon
	 */
	public SignedBeaconDTO(Vector2D pos, Vector2D vel, Timestamp timestamp, X509Certificate senderCert, PrivateKey pKey) {
		this.setCertificate(senderCert);
		this.beaconDTO = new BeaconDTO(pos, vel, timestamp);
		this.sign(pKey);
	}

	/**
	 * Builds a signed BeaconDTO
	 * @param	Vector2D			position vector
	 * @param	Vector2D			velocity vector
	 * @param	X509Certificate		certificate the entity sending this beacon
	 * <p><b>NOTE:</b> since timestamp is omited a new one is created with current time</p>
	 */
	public SignedBeaconDTO(Vector2D pos, Vector2D vel, X509Certificate senderCert, PrivateKey pKey) {
		this.setCertificate(senderCert);
		this.beaconDTO = new BeaconDTO(pos, vel);
		this.sign(pKey);
	}

//  ------- GETTERS  ------------
	public BeaconDTO beaconDTO() { return this.beaconDTO; }
	public Vector2D getPosition() { return this.beaconDTO.getPosition(); }
	public Vector2D getVelocity() { return this.beaconDTO.getVelocity(); }
	public Timestamp getTimestamp() { return this.beaconDTO.getTimestamp(); }
	public String toString() { return this.beaconDTO.toString(); }

	/**
	 * Returns the serialized value of this DTO
	 */
	@Override
	public byte[] serialize() {
		// Join serializations
		byte[] serializedDTO = this.beaconDTO.serialize();
		byte[] serializedCert = this.senderCertificate.toString().getBytes();
		byte[] newSerialization = new byte[serializedDTO.length + serializedCert.length];
		System.arraycopy(serializedDTO, 0, newSerialization, 0, serializedDTO.length);
		System.arraycopy(serializedCert, 0, newSerialization, serializedDTO.length, serializedCert.length);
		return newSerialization;
	}

	/**
	 * Generates the signature for the DTO + senderCertificate in the constructor with given PrivateKey
	 * @param	PrivateKey	key used to signed this DTO
	 * @return	returns the signature generated
	 *
	 * <p><b>NOTE:</b> This also sets the internal atribute <b>signature</b> that can be acessed with <b>getSignature</b></p>
	 */
	@Override
	public byte[] sign(PrivateKey pKey) {
		byte[] serializedVal = this.serialize();
		byte[] sig = null;
		try {
			sig = Resources.makeDigitalSignature(serializedVal, pKey); }
		catch (Exception e) {
			System.out.println(Resources.ERROR_MSG("Failed to create signature: "+e.getMessage()));
			return null;
		}
		this.signature = sig;
		return this.signature;
	}

	/*
	 * Verify if the senderCertificate was signed by the given entity
	 * @param	X509Certificate	entity used to verify the senderCertificate in this DTO (usually the CA)
	 * @return	returns true if it was signed by given entity
	 */
	@Override
	public boolean verifyCertificate(X509Certificate otherEntity) {
		return Resources.verifySignedCertificate(this.senderCertificate, otherEntity.getPublicKey());
	}

	/**
	 * Compares the received signature with the calculated DTO signature
	 * @return	returns true if the signature is correct
	 */
	@Override
	public boolean verifySignature() {
		try { Resources.verifyDigitalSignature(this.signature, this.serialize(), this.senderCertificate.getPublicKey()); }
		catch (Exception e ) { return false; } // certificate was not signed by sender
		return true;
	}
}

