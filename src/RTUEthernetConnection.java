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
	
	public Response getResponse() throws IOException {
	    return new Response(is.readAllBytes());
	}
	
//	public void castingExample() throws IOException {
//	    RTUEthernetConnection rtu = new RTUEthernetConnection(socket);
//	    ReadCoilStatusResponse rd = (ReadCoilStatusResponse)rtu.getResponse();
//	    rd.getCoil(3);
//	}
	
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

class Response{
     byte[] data;
     byte[] exractedData;
    public Response(byte[] data) {
        this.data = data;
        this.exractedData = extractData(this.data);
    }
    public static int extractSlaveAddress(byte[] response) {
        return response[0] & 0xFF; // Extract and return the slave address from the response frame
    }

    public static int extractFunctionCode(byte[] response) {
        return response[1] & 0xFF; // Extract and return the function code from the response frame
    }

    public static byte[] extractData(byte[] response) {
        // Extract and return the data portion of the response frame
        int dataLength = response.length - 5; // Calculate the length of the data portion (excluding address, function code, byte counts and CRC)
        byte[] data = new byte[dataLength];
        System.arraycopy(response, 3, data, 0, dataLength);
        return data;
    }

    public static boolean verifyCRC(byte[] response) {
        // Calculate CRC for received response frame and compare with the CRC in the frame
        CRC32 crc32 = new CRC32();
        crc32.update(response, 0, response.length - 2);
        long computedCRC = crc32.getValue();
        long receivedCRC = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
        return computedCRC == receivedCRC;
    }
}

class ReadCoilStatusRequest extends Request{
	int slaveAddress;
	int startingAddress;
	int quantityOfCoils;
	
	public ReadCoilStatusRequest(int slaveAddress, int startingAddress, int quantityOfCoils) {
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

class ReadCoilStatusResponse extends Response{
    public ReadCoilStatusResponse(byte[] data) {
        super(data);
        // TODO Auto-generated constructor stub
    }

    public boolean getCoil(int index) throws CRCVerificationException {
       if(verifyCRC(data)) {
           if (index < 0 || index >= exractedData.length * 8) {
               throw new IllegalArgumentException("Index is out of range.");
           }
           int byteIndex = index / 8;
           int bitIndex = index % 8;
           byte mask = (byte) (1 << bitIndex);
           return (exractedData[byteIndex] & mask) != 0;
       }else {
           throw new CRCVerificationException("CRC Verification for Read coil failed");
       }
    }
}

class ReadInputStatusRequest extends Request{
	int slaveAddress;
	int startingAddress;
	int quantityOfInputs;
	
	public ReadInputStatusRequest(int slaveAddress, int startingAddress, int quantityOfInputs) {
		this.slaveAddress = slaveAddress;
		this.startingAddress = startingAddress;
		this.quantityOfInputs = quantityOfInputs;
	}
	@Override
	public byte[] dataBuff() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) slaveAddress);
        buffer.put((byte) 0x02); // Function Code 02: Read Input Status
        buffer.putShort((short) startingAddress);
        buffer.putShort((short) quantityOfInputs);
        return addCRC(buffer.array());
	}

	
}

class ReadInputStatusResponse extends Response{
    public ReadInputStatusResponse(byte[] data) {
        super(data);
        // TODO Auto-generated constructor stub
    }

    public void getInput() {
        
    }
}

class ReadHoldingRegisterRequest extends Request{
    int slaveAddress;
    int startingAddress;
    int quantityOfRegisters;
    
    public ReadHoldingRegisterRequest(int slaveAddress, int startingAddress, int quantityOfRegisters) {
        this.slaveAddress = slaveAddress;
        this.startingAddress = startingAddress;
        this.quantityOfRegisters = quantityOfRegisters;
    }
	@Override
	public byte[] dataBuff() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) slaveAddress);
        buffer.put((byte) 0x03); // Function Code 03: Read Holding Registers
        buffer.putShort((short) startingAddress);
        buffer.putShort((short) quantityOfRegisters);
        return addCRC(buffer.array());
	}

}

class ReadInputRegisterRequest extends Request{
    int slaveAddress;
    int startingAddress;
    int quantityOfRegisters;
    
    public ReadInputRegisterRequest(int slaveAddress, int startingAddress, int quantityOfRegisters) {
        this.slaveAddress = slaveAddress;
        this.startingAddress = startingAddress;
        this.quantityOfRegisters = quantityOfRegisters;
    }
	@Override
	public byte[] dataBuff() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) slaveAddress);
        buffer.put((byte) 0x04); // Function Code 04: Read Input Registers
        buffer.putShort((short) startingAddress);
        buffer.putShort((short) quantityOfRegisters);
        return addCRC(buffer.array());
	}
	
}

class WriteSingleCoilRequest extends Request{
    int slaveAddress;
    int outputAddress;
    boolean outputValue;
    
    public WriteSingleCoilRequest(int slaveAddress, int outputAddress, boolean outputValue) {
        this.slaveAddress = slaveAddress;
        this.outputAddress = outputAddress;
        this.outputAddress = outputAddress;
    }
	@Override
	public byte[] dataBuff() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) slaveAddress);
        buffer.put((byte) 0x05); // Function Code 05: Force Single Coil
        buffer.putShort((short) outputAddress);
        buffer.put(outputValue ? (byte) 0xFF : (byte) 0x00);
        return addCRC(buffer.array());
	}

	
}

class WriteSingleRegisterRequest extends Request{
    int slaveAddress;
    int registerAddress;
    int registerValue;
    
    public WriteSingleRegisterRequest(int slaveAddress, int registerAddress, int registerValue) {
        this.slaveAddress = slaveAddress;
        this.registerAddress = registerAddress;
        this.registerValue = registerValue;
    }
	@Override
	public byte[] dataBuff() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) slaveAddress);
        buffer.put((byte) 0x06); // Function Code 06: Preset Single Register
        buffer.putShort((short) registerAddress);
        buffer.putShort((short) registerValue);
        return addCRC(buffer.array());
	}
}
@SuppressWarnings("serial")
class CRCVerificationException extends Exception {
    public CRCVerificationException(String message) {
        super(message);
    }
}