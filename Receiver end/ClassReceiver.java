package receiverSide;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 *@file ClassReceiver.java
 *@author Mrinmayi_Sadavarte
 *@brief This program acts as a Receiver and receives Packets over UDP socket and send Acknowledgement for the received packet.
 */
public class ClassReceiver {

	private static byte [] receivedPacket = null; 	// Received packet
	private static byte [] sendACK;					// ACK byte array to be sent
	private static int MPS = 30;					// maximum length of data
	private static byte[] calculatedIntegrityChk = new byte [4];

	public static void main(String[] args) throws IOException
	{
		System.out.println("***Welcome to the RECEIVER of TX-RX Communication over UDP Project***");

		int sequenceNum = 10000;
		int flag = 0; 			 // flag, set to 1 if any error in the received packet
		int index = 0; 			 // index used to form the final combined data
		byte [] dataFromAllPackets = new byte [500];  // data received from transmitter
		sendACK = new byte[9];   // Creating Acknowledgement array to send
		byte[] receivedIntChk = new byte [4]; //to store calculated integrity check value

		// Creating a receive packet array
		receivedPacket = new byte [40]; // Maximum size of the received packet

		// Creating an object of the UtilityReceiver Class
		UtilityReceiver obj1 = new UtilityReceiver();

		// DatagramPacket Receive object
		DatagramPacket DatagramPacketrecvObj = new DatagramPacket(receivedPacket, receivedPacket.length);

		// Creating a DatagramSocket object
		DatagramSocket DatagramSocketObj = new DatagramSocket(9909);

		/**
		 * This while loop runs until flag is set or all packets are received
		 */
		while(flag == 0)
		{
			int errorFlag = 0; // Is set if there is any error in the received packet

			// Receiving the packet
			DatagramSocketObj.receive(DatagramPacketrecvObj);

			// Creating an object of the InetAddress class
			InetAddress objInetAddress = DatagramPacketrecvObj.getAddress();

			// DatagramPacket Send object and extracting client IP address, port number from the received packet
			DatagramPacket DatagramPacketsendObj = new DatagramPacket(sendACK, sendACK.length, objInetAddress, DatagramPacketrecvObj.getPort());

			// Filling receivedPacket by arrived packet information
			receivedPacket = DatagramPacketrecvObj.getData();

			/*
			 * Going in and checking each field of the received packet in the following nested if-else loops
			 */
			if (receivedPacket[0] == 0x55 || receivedPacket[0] == (byte) 0xaa)	// checking the packet type field
			{
				int receivedSeqNum = obj1.byteArrayToInt(receivedPacket);	// calling byteArrayToInt method to obtain sequence number in int
				int receivedPayLen = (receivedPacket[5] & 0xff);			// length of received payLoad

				/*
				 * Verifying the sequence number. receivedSeqNum may be equal to sequenceNum OR
				 * Less than the sequenceNum if ACK of the previous packet is lost and the same packet is re transmitted by the transmitter
				 */
				if (receivedSeqNum <= sequenceNum)
				{
					// Verify if the receivedPayLenght is less than or equal to the MPS // less for the last packet
					if (receivedPayLen <= MPS)
					{
						/*
						 * This following 'if'-block is executed for the last packet and the 'else'-block is executed for rest of the packets
						 */
						if (receivedPacket[0] == (byte) 0xaa)	// for last packet
						{
							// Extracting header and data of last packet
							byte[] lastPacket = new byte [receivedPacket.length - 14];
							for (int i = 0; i < receivedPacket.length - 14; i++)
							{
								lastPacket[i] = receivedPacket[i];
							} // Header extracted

							// extracting arrived integrity values only
							for (int i = receivedPacket.length-14; i < receivedPacket.length-10; i++)
							{
								receivedIntChk [i-(receivedPacket.length-14)] = receivedPacket [i];
							} //Integrity values extracted


							// Calculating integrity at the Receiver
							obj1.initialState();
							obj1.initialPermutation();
							calculatedIntegrityChk = obj1.streamDetection(lastPacket);

							// Verifying if the calculatedIntegrityChk is equal to the received integrity. If yes then send ACK
							if(Arrays.equals(receivedIntChk, calculatedIntegrityChk))
							{
								// to print the last packet
								System.out.print("\nLast packet received: [");

								for(int i = 0; i < receivedPacket.length-10; i++)
								{
									System.out.print(receivedPacket[i] + ", ");

								}
								System.out.println("]");

								byte [] sendACK = toSendACK(); // Calling the toSendACK method

								// Sending the ACK packet
								DatagramSocketObj.send(DatagramPacketsendObj);

								System.out.println("Sent ACK Packet is :" + Arrays.toString(sendACK));

								// Filling up the data array with data received in the last packet
								for(int i = 6; i < lastPacket.length; i++)
								{
									dataFromAllPackets [index] = lastPacket[i];
									index++;
								}

								System.out.println("\nFinal Data received from transmitter is :" + Arrays.toString(dataFromAllPackets));

								flag = 1;
								DatagramSocketObj.close();
							}
						}	// last packet ends
						else // for packets other than last packet
						{
							byte [] otherPacket = new byte [receivedPacket.length-4];

							// Extracting header and data of other packets
							for (int i = 0; i < receivedPacket.length - 4; i++)
							{
								otherPacket[i] = receivedPacket[i];
							}// Extracting ends

							// Extracting arrived integrity check value only
							int j = 0;
							for (int i = receivedPacket.length-4; i < receivedPacket.length; i++)
							{
								receivedIntChk [j] = receivedPacket [i];
								j++;
							}// Extracting integrity ends

							// Calculating Integrity for the received packet
							obj1.initialState();
							obj1.initialPermutation();
							calculatedIntegrityChk = obj1.streamDetection(otherPacket);

							// Verifying if the calculatedIntegrityChk is equal to the received integrity. If yes then send ACK
							if(Arrays.equals(receivedIntChk, calculatedIntegrityChk))
								{
									System.out.println("\nPacket received:" + Arrays.toString(receivedPacket));

									byte [] sendACK = toSendACK(); // Calling the toSendACK method

									// Sending the ACK packet
									DatagramSocketObj.send(DatagramPacketsendObj);
									System.out.println("Sent ACK Packet is :" + Arrays.toString(sendACK));

									// Filling up the data array with data received in the packet
									for(int i = 6; i < otherPacket.length; i++)
									{
										dataFromAllPackets [index] = otherPacket[i];
										index++;
									}

								}
						} // else for sending ACK for other than last packet ends here

					}else
						{
							errorFlag = 1;	// payload length > MPS
						}
					// Incrementing the sequenceNumber if receved ACK is perfect
					if(receivedSeqNum == sequenceNum)
						sequenceNum += receivedPayLen;
				}else
					errorFlag = 1; // sequence number does not match

			}else
				errorFlag = 1; // packet type invalid

			if(errorFlag ==1)
			{
				System.out.println("Corrupt Packet: Discarded....!!");
				for (int i=0; i < receivedPacket.length; i++)	// discarding incorrect packet
				receivedPacket[i] = 0;
			}
		}//while loop ends

}// main ends

/**
 * @brief This method forms the Acknowledgement packet
 * @return filled up sendACK array which is to be sent.
 */
	public static byte[] toSendACK()
	{
		// creating an object of the class UtilityReceiver
		UtilityReceiver obj2 = new UtilityReceiver ();

		// setting PacketType
		sendACK [0] = (byte) 0xff;

		// Putting the ACK bytes in the ACK packet
		int seqNumInt = obj2.byteArrayToInt (receivedPacket);
		int ACKNumInt = seqNumInt + (receivedPacket[5] & 0xff);
		byte[] ACKNumByte = obj2.intToByteArray (ACKNumInt);	// calling intToByteArray method
		for (int i = 1; i <= 4; i++)
		{
			sendACK [i] = ACKNumByte [i-1];	// ACK number in ACK packet
		}
		// ACK bytes filled up

		// FOr calculating the integrity and filling it up in the sendACK array Packet
		byte [] tempArray = new byte[5];
		for(int i = 0; i < 5; i++)
			tempArray[i] = sendACK[i];

		obj2.initialState();
		obj2.initialPermutation();
		byte integrityChkACK [] = obj2.streamDetection(tempArray);
		// filling up in sendACK packet
		for (int i = 5; i < 9; i++)
		{
			sendACK [i] = integrityChkACK [i-5];	// ACK number in ACK packet
		}
		// ACK packet filledup and returned
		return sendACK;
	}

} // Class Receiver Ends here

