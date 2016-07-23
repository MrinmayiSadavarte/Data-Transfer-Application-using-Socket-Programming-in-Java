package TransmitterSide;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Random;

/**
 *@file ClassTransmitter.java
 *@author Mrinmayi_Sadavarte
 *@brief This program acts as a transmitter and sends data over UDP socket ensuring cryptographic Verification
 */
public class ClassTransmitter {

	private final static int MPS = 30;			   //check if we require it to be global
	private final static int MAXDATA = 500; 	   //Maximum number of bytes to be sent over the network
	private static InetAddress localHost;  // creating an object of the InetAddress class
	private static byte packetType;   	   //0x55 or 0xaa
	private static int seqNum = 10000;     //some random starting sequence number 32 bit maximum
	private static byte lenPayLoad ; 	   // length of data payload in each packet
	private static DatagramSocket DatagramSocketObj;  // creating an object of the DatagramSocket class
	private static int timeOutFlag = 0;    // 0 means no timeout
	private static int timeOutCount;       // to count the number of timeouts
	private static int timeOut = 1000;     // timeout flag
	private static int flag =0;
	private static byte toSendPacket[]=null;
	private static int ptr = 0;

	public static void main(String[] args) throws Exception
	{
		// Welcome and Initialization of variables
		System.out.println("***Welcome to the ENTS-640 Project***");

		int lastPtr = 29;
		byte payLoad[] = null;
		ClassTransmitter txObj = new ClassTransmitter();

		// Creating and printing a data byte array 500 bytes
		byte[] dataByteArray = new byte[MAXDATA];
		Random giveRandom = new Random();
		giveRandom.nextBytes(dataByteArray);
		// Print the Data Stream to be sent
		System.out.println("Data to be Sent is: " + Arrays.toString(dataByteArray));

		localHost = InetAddress.getLocalHost();

		/**
		 * This do-while loop takes care of taking data chunk and sending the packets and receiving the ACKs(Stop and Wait)
		 * It checks for the timeout conditions and the Packet sequence
		 */
		do
		{
			timeOutCount = 0; // For every packet sent timeOut count is set to Zero

			/*
			 *The function of this if-else is extracting data-chunk from the data stream
			 */
			if((MAXDATA-ptr) >= 30 ) // Check if 30 bytes are available // Executes except for the last 20 bytes
			{
				lenPayLoad = (byte) (lastPtr - ptr + 1);
				payLoad = new byte[lenPayLoad];

				int i = 0;
				for( ; ptr <= lastPtr ; ptr++ )
				{
					payLoad[i] = dataByteArray[ptr];   // Extracting last remaining bytes of data from the data stream
					i++;
				}
				lastPtr += 30;

			}else // This thing will execute only for the last packet which is less than 30 bytes
			{
				lenPayLoad = (byte) (MAXDATA - ptr);
				payLoad = new byte[lenPayLoad];

				int i = 0;
				for( ; ptr < MAXDATA ; ptr++ )
				{
					payLoad[i] = dataByteArray[ptr];    // Extracting 30 bytes of data from the data stream
					i++;
				}
			}// if-else ends here // data has been extracted here for one packet in one-go of this if-else

			/**
			 * Does not break the loop if timeOut happens or Integrity doesn't match or the packet type is not correct(Retransmits the same packet)
			 */
			do
			{
				timeOutFlag = 0;
				flag = 0;

				// Calling the sendPacket Method to send the packets
				txObj.sendPacket(payLoad);

				// Calling the method to put timer and receive the ACK packet
				if ((lenPayLoad + seqNum) == txObj.receivePacket())
				{
					flag = 0; // ACK in sequence
					seqNum += lenPayLoad ;
				}else
				{
					flag = 1;
				}

			}while(timeOutFlag ==1 || flag == 1); // Retransmits the packet if timeout or ACK is corrupt

		}while(packetType != (byte) 0xaa); 		  // This loop takes care of sending packets, breaks when last packet is sent and ACK is received and checked

		// Closing the datagram socket
		DatagramSocketObj.close();
		System.out.println("****End of the Communication over a UDP Socket Program****");

	} // main ends here


/**
 * @brief This method attaches the header, calls methods to calculate Integrity, attaches the integrity check and sends the packet taking care of the timeOut
 * @param payLoad
 * @throws IOException
 */
	public void sendPacket(byte[] payLoad) throws IOException
	{
		byte inputIntegrityArray[] = new byte[lenPayLoad + 6]; // header + payLoad
		toSendPacket= new byte[lenPayLoad + 10]; // whole packet

		// Creating an object of the UtilityTransmitter Class
		UtilityTransmitter objUtility = new UtilityTransmitter ();

		// To put in the packet type
		if (lenPayLoad == MPS )
		{
			packetType = 0x55;
		}else
		{
			packetType = (byte) 0xaa; // When the length of payLoad is less than MSS
		}
		inputIntegrityArray[0] = packetType;

		// To put in sequence number
		byte toFill[] = new byte[4];
		toFill = objUtility.intToByteArray(seqNum);
		for(int i = 1 ; i <=4 ; i++)
		{
			inputIntegrityArray[i]= toFill[i-1];
		}

		// To put in the length of payload
		inputIntegrityArray[5] = lenPayLoad;

		// To add the data in the inputIntegrityArray array
		for (int i = 6 ; i < (lenPayLoad + 6) ; i++)
		{
			inputIntegrityArray[i] = payLoad[i-6];
		}

		// Calculation of integrity for the toSend packet
		objUtility.initialState();
		objUtility.initialPermutation();
		byte[] intCheckToSend = new byte[4];
		intCheckToSend = objUtility.streamGeneration(inputIntegrityArray);

		// Copy payload and the integrity bytes
		int j = 0;
		for(int i = 0 ; i < (lenPayLoad + 10) ; i++)
		{
			if (i<lenPayLoad+6)   // For copying upto payLoad
			{
				toSendPacket[i]= inputIntegrityArray[i];
			}else
			{
				// For copying the integrity check bits
				toSendPacket[i] = intCheckToSend[j];
				j++;
			}
		} // Filled up the toSendPacket array

		System.out.println("\nThe sent packet is: " + Arrays.toString(toSendPacket));

		// Creating DatagramPacket object
		DatagramPacket DatagramPacketSendObj = new DatagramPacket(toSendPacket, (lenPayLoad + 10), localHost, 9909);

		//Creating a DatagramSocket object
		DatagramSocketObj =new DatagramSocket();

		// Sending a UDP packet to the Receiver
		DatagramSocketObj.send(DatagramPacketSendObj);

	}

/**
 * @brief This method receives the ACK packets checks the integrity and takes care of the timeout counts
 * @return Returns receivedACK number
 * @throws IOException
 */
	public int receivePacket() throws IOException
	{
		int receivedACK = 0; // Returned variable: received sequence number
		flag = 0; 			 // Error flag: to be set 1 if we have any error in the received packet
		UtilityTransmitter objUtility = new UtilityTransmitter();   // Creating object of the UtilityTransmitter class

		// Initializing a byte array and DatagramPacket object for receiving data
		byte[] rcvdByteArray = new byte[9];
		DatagramPacket DatagramPacketRcvdObj = new DatagramPacket(rcvdByteArray, 9);

		// Setting timer for timeout as 1 second initially
		DatagramSocketObj.setSoTimeout(timeOut);

		try
		{
			// Receiving packet over the network
			DatagramSocketObj.receive(DatagramPacketRcvdObj);

		}catch(SocketTimeoutException e)
		{
			System.out.println("ERROR....!!!!! SocketTimeoutException:  Timeout");

			timeOutFlag = 1; // flag raised: time out..!!
			timeOutCount++;  // Incrementing the number of timeout counter
			timeOut *= 2;	 // Making the timeout time double than the previous timeout

			if (timeOutCount == 4) // Stop the communication if number of attempts to send any one packet are equal to four
			{
				System.out.println("Communication Failure.......!!!!!");
				DatagramSocketObj.close();
				System.exit(0);
			}
		}

		// To get the ACK number
		if (timeOutFlag!=1)
		{
			byte toConvert[]=new byte[4];	// Temporary array used to send parameters to the conversion methods
			for(int i = 0 ; i <4 ; i++)
			{
				toConvert[i]=rcvdByteArray[i+1] ;
			}
			receivedACK = objUtility.byteArrayToInt(toConvert); // Received ACK number(sequence number) in int

			// To extract received integrity
			byte[] receivedIntegrity = new byte[4];
			for(int i = 0 ; i <4; i++)
			{
				receivedIntegrity[i]=rcvdByteArray[i+5];
			}

			// To calculate the integrity at the transmitter for comparing with the received Integrity bytes
			byte[] data = new byte[5];
			for(int i = 0 ; i <5 ; i++)
			{
				data[i] = rcvdByteArray[i];
			}
			objUtility.initialState();
			objUtility.initialPermutation();
			if(Arrays.equals(receivedIntegrity, objUtility.streamGeneration(data)))  // Checking if the calculated integrity is equal to the received integrity
			{
				flag = 0;
			}else flag = 1; // Set flag = 1 if integrity doesn't match

			if (rcvdByteArray[0] == (byte) 0xff )
			{
				flag = 0;
			}else flag =1; // Set flag = 1 if packet type doesn't match

			System.out.println("Received ACK Packet is: " + Arrays.toString(rcvdByteArray));
		}

		return receivedACK;


	} // Receive packet method ends here

}// class ends here
