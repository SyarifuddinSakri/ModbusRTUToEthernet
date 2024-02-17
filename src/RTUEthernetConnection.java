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
     byte[] extractedData;
    public Response(byte[] data) {
        this.data = data;
        try {
			this.extractedData = extractData(this.data);
		} catch (ModbusException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    public static int extractSlaveAddress(byte[] response) {
        return response[0] & 0xFF; // Extract and return the slave address from the response frame
    }

    public static int extractFunctionCode(byte[] response) {
        return response[1] & 0xFF; // Extract and return the function code from the response frame
    }

    public static byte[] extractData(byte[] response) throws ModbusException {
        // Extract and return the data portion of the response frame
        int dataLength = response.length - 5; // Calculate the length of the data portion (excluding address, function code, byte counts and CRC)
        byte[] data = new byte[dataLength];
        System.arraycopy(response, 3, data, 0, dataLength);
        if (data.length>1) {
        	return data;
        }else {
        	if(data[0]==(byte)0x01) {
        		throw new ModbusException("Illegal Function : Exception code "+byteToHexString(data[0]));
        	}else if(data[0]==(byte)0x02) {
        		throw new ModbusException("Illegal Data Address : Exception code "+byteToHexString(data[0]));
        	}else if(data[0]==(byte)0x03) {
        		throw new ModbusException("Illegal Data Value : Exception code "+byteToHexString(data[0]));
        	}else if(data[0]==(byte)0x04) {
        		throw new ModbusException("Slave Device Failure : Exception code "+byteToHexString(data[0]));
        	}else if(data[0]==(byte)0x05) {
        		throw new ModbusException("Acknowleging and taking more time to process : Exception code "+byteToHexString(data[0]));
        	}else if(data[0]==(byte)0x06) {
        		throw new ModbusException("Slave Device Busy : Exception code "+byteToHexString(data[0]));
        	}else if(data[0]==(byte)0x0A){
        		throw new ModbusException("Gateway Path Unavailable : Exception code"+byteToHexString(data[0]));
        	}else if(data[0]==(byte)0x0B) {
        		throw new ModbusException("Gateway Target Device Failed to Respond : Exception code "+byteToHexString(data[0]));
        	}else {
        		throw new ModbusException("Exception Occured : Exception code "+byteToHexString(data[0]));
        	}
        }
    }

    public static boolean verifyCRC(byte[] response) {
        // Calculate CRC for received response frame and compare with the CRC in the frame
        CRC32 crc32 = new CRC32();
        crc32.update(response, 0, response.length - 2);
        long computedCRC = crc32.getValue();
        long receivedCRC = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
        return computedCRC == receivedCRC;
    }
    
    public boolean getBooleanAtIndex(int index) throws CRCVerificationException {
        if(verifyCRC(data)) {
            if (index < 0 || index >= extractedData.length * 8) {
                throw new IllegalArgumentException("Index is out of range.");
            }
            int byteIndex = index / 8;
            int bitIndex = index % 8;
            byte mask = (byte) (1 << bitIndex);
            return (extractedData[byteIndex] & mask) != 0;
        }else {
            throw new CRCVerificationException("CRC Verification for Read coil failed");
        }
     }
    
    public int getValueAtIndex(int index) throws CRCVerificationException{
        if(verifyCRC(data)) {
            // Check if the byte array length is even (each integer is represented by 2 bytes)
            if (extractedData.length % 2 != 0) {
                throw new IllegalArgumentException("Byte array length must be even");
            }
            
            int[] result = new int[extractedData.length / 2]; // Create an array to store the integers
            
            for (int i = 0; i < extractedData.length; i += 2) {
                // Combine the pair of bytes into an integer
                result[i / 2] = (extractedData[i] << 8) | (extractedData[i + 1] & 0xFF); // Use bitwise OR to combine bytes
            }
            return result[index];
        }else {
            throw new CRCVerificationException("CRC Verification for Read coil failed");
        }
    }
    public static String byteToHexString(byte b) {
        // Convert the byte to an integer to ensure correct conversion to hexadecimal
        int intValue = b & 0xFF; // Convert to unsigned byte
        
        // Convert the integer value to a hexadecimal string
        String hexString = Integer.toHexString(intValue);
        
        // If the hexadecimal string has only one character, prepend a leading zero
        if (hexString.length() == 1) {
            hexString = "0" + hexString;
        }
        
        return hexString.toUpperCase(); // Convert to uppercase for consistency
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

    public boolean getCoil(int index) throws CRCVerificationException, ModbusFunctionVerificationException {
    	if (extractFunctionCode(data)==1) {
        	return getBooleanAtIndex(index);
    	}else {
    		throw new ModbusFunctionVerificationException("Data is not the function code 01 : Read Coil Status");
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

    public boolean getInput(int index) throws CRCVerificationException, ModbusFunctionVerificationException {
    	if(extractFunctionCode(data)==2) {
        	return getBooleanAtIndex(index);
    	}else {
    		throw new ModbusFunctionVerificationException("Data is not the function code 02 : Read Input Status");
    	}

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

class ReadHoldingRegisterResponse extends Response{

	public ReadHoldingRegisterResponse(byte[] data) {
		super(data);
		// TODO Auto-generated constructor stub
	}
	
	public int getHoldingRegister(int index) throws CRCVerificationException, ModbusFunctionVerificationException {
		if (extractFunctionCode(data)==3) {
			return getValueAtIndex(index);
		}else {
			throw new ModbusFunctionVerificationException("Data is not the function code 03 : Read Holding Register");
		}

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

class ReadInputRegisterResponse extends Response{

	public ReadInputRegisterResponse(byte[] data) {
		super(data);
		// TODO Auto-generated constructor stub
	}
	public int getInputRegister(int index) throws CRCVerificationException, ModbusFunctionVerificationException {
		if(extractFunctionCode(data)==4) {
			return getValueAtIndex(index);
		}else {
			throw new ModbusFunctionVerificationException("Data is not the function code 04 : Read Input Register");
		}

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
@SuppressWarnings("serial")
class ModbusFunctionVerificationException extends Exception {
    public ModbusFunctionVerificationException(String message) {
        super(message);
    }
    
}
@SuppressWarnings("serial")
class ModbusException extends Exception {
    public ModbusException(String message) {//code 01
        super(message);
    }
}