import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

public class RTUEthernetConnection {
	Socket socket;
	OutputStream os;
	InputStream is;
	public RTUEthernetConnection(Socket socket) throws IOException {
		this.socket = socket;
		this.os = this.socket.getOutputStream();
		this.is = this.socket.getInputStream();
	}
	
	public void setRequest(Request request) throws IOException {
		os.write(request.dataBuff());
		os.flush();
	}
	
	public void makan() {
	
	}
}

abstract class Request{
	public Request() {
		
	}
	
	public abstract byte[] dataBuff();
	
	
    protected static byte[] addCRC(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data, 0, data.length - 2);
        long crcValue = crc32.getValue();
        data[data.length - 2] = (byte) ((crcValue >> 8) & 0xFF);
        data[data.length - 1] = (byte) (crcValue & 0xFF);
        return data;
    }
}

class ReadCoilStatus extends Request{
	int slaveAddress;
	int startingAddress;
	int quantityOfCoils;
	
	public ReadCoilStatus(int slaveAddress, int startingAddress, int quantityOfCoils) {
		this.slaveAddress = slaveAddress;
		this.startingAddress = startingAddress;
		this.quantityOfCoils = quantityOfCoils;
	}
	@Override
	public byte[] dataBuff() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) slaveAddress);
        buffer.put((byte) 0x01); // Function Code 01: Read Coil Status
        buffer.putShort((short) startingAddress);
        buffer.putShort((short) quantityOfCoils);
        return addCRC(buffer.array());
	}
}

class ReadInputStatus extends Request{
	int slaveAddress;
	int startingAddress;
	int quantityOfStatus;
	
	public ReadInputStatus(int slaveAddress, int startingAddress, int quantityOfStatus) {
		this.slaveAddress = slaveAddress;
		this.startingAddress = startingAddress;
		this.quantityOfStatus = quantityOfStatus;
	}
	@Override
	public byte[] dataBuff() {
		// TODO Auto-generated method stub
		return null;
	}

	
}

class ReadHoldingRegister extends Request{

	@Override
	public byte[] dataBuff() {
		// TODO Auto-generated method stub
		return null;
	}

}

class ReadInputRegister extends Request{

	@Override
	public byte[] dataBuff() {
		// TODO Auto-generated method stub
		return null;
	}
	
}

class WriteSingleCoil extends Request{

	@Override
	public byte[] dataBuff() {
		// TODO Auto-generated method stub
		return null;
	}

	
}

class WriteSingleRegister extends Request{

	@Override
	public byte[] dataBuff() {
		// TODO Auto-generated method stub
		return null;
	}
	
}