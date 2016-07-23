package receiverSide;

/**
 *@file UtilityReceiver.java
 *@author Mrinmayi_Sadavarte
 *@brief This program acts as utility to the ClassReceiver
 */
public class UtilityReceiver {

	static byte [] S;
	static byte [] T;
	static byte [] K;

/**
 * @brief This method initializes the S vector
 */
	public void initialState()
	{
		// Initialization of S vector

		 S = new byte [256];
		 T = new byte [256];
		 K =new byte[] {55,28,-114,67,-61,05,-38,-91,-91,-38,05,-61,67,-114,28,55}; // Hardcoded Key to be used for the integrity calculations
		 int keylen = K.length; // length of the key

		for (int i = 0; i < 256; i++)
		{
			S[i] = (byte)i;
			T[i] = K[i % keylen];
		}
	}

/**
 * @brief This method calculates initial permutations of the S vector
 */

	public void initialPermutation()
	{
		// Initial permutation of S vector
		int j = 0;
		for (int i = 0; i < 256; i++)
		{
			j = Math.abs((j + S[i] + T[i]) % 256);
			byte temp = S[i];
			S[i] = S[j];
			S[j] = temp;
		}
	}

/**
 * @brief This method calculates the Key-Stream, produces the Ciphertext, compresses it to 4 bytes and returns it.
 * @return compressed : four byte Ciphertext
 */

	public byte[] streamDetection(byte[] arrivedPacket)
	{
		byte[] recPacket = new byte [arrivedPacket.length];

		// copying the contents of arrivedPacket into recPacket
		for (int i = 0; i < arrivedPacket.length; i++)
			recPacket [i] = arrivedPacket [i];

		byte [] temp = null;

		// zero padding
		if((recPacket.length % 4) != 0)	// for the last packet & also for all ACK packets
		{
			int n = recPacket.length;
			while(n % 4!=0)
			{
				n++;
			}

			temp = new byte [n];	// creating an array of size = length of the sequence

			for(int i = 0; i < recPacket.length ; i++)
				temp[i] = recPacket[i]; // copying the data and header in the array

			for(int i = recPacket.length; i < n ; i++)
				temp[i] = 0; // zero padding the remaining elements in the array

		}
		else	// for all other packets except the last
		{
			temp = new byte [recPacket.length];
			for(int i = 0; i < recPacket.length ; i++)
				temp[i] = recPacket[i]; // copying the data and header in the array
		}


		// keystream 'k' generation
		byte[] cipherText = new byte[temp.length];
		byte [] compressed = new byte[4];
		int x = 0;   // index required for generation
		int y = 0;   // index required for generation
		int z = 0;   // index for the cipherText

		while (z < temp.length)
		{
			x = Math.abs((x + 1) % 256);
			y = Math.abs((y + S[x]) % 256);
			byte temp1 = S[x];
			S[x] = S[y];
			S[y] = temp1;
			int t = Math.abs((S[x] + S[y]) % 256);
			byte k = S[t];	// keystream is formed

			// Forming the ciphertext
			cipherText[z] = (byte)(temp[z] ^ k );
			z++;
		}

		// compress the cipher text stream
		for(int i = 0 ; i < 4; i++)
		{
			for(int j = i; j < temp.length; j+=4)
			{
				compressed[i] ^=  temp[j];
			}
		}

		return compressed;
	}//Stream generation ends here


/**
 * @brief This method converts a byte array in to equivalent integer value
 * @return converted: converted byte array
 */
	public int byteArrayToInt (byte byteArray[])
	{
		int intValue = 0;
		for (int i = 0; i < 4; i++)
		{
			intValue = intValue << 8;
			intValue = intValue | (byteArray[i+1] & 0xff);// to make sure that we dont have any signed bits in the MSB
		}
		return intValue;
	}

/**
 * @brief This method converts integer to byte array
 * @return converted: converted byte array
 */
	public byte [] intToByteArray (int integer)
	{
		byte [] converted = new byte [4];
	    // shifting the bits towards right and extracting lower 8 bits as a byte in the byte array
		for (int j = 0; j<4; j++)
		{
			converted [3-j] = (byte) (integer >> (8*j));// no need of logical right sift because we donot need the signed bit since seq numbers are always positive
		}

		return converted;
	}

} // class ends here
