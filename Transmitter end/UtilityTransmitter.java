package TransmitterSide;

/**
 *@file UtilityTransmitter.java
 *@author Mrinmayi_Sadavarte
 *@brief This program acts as a Utility to the ClassTransmitter
 */
public class UtilityTransmitter{

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
		 int keylen = K.length;  // Length of the key

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
		byte temp;
		int j = 0;
		for (int i = 0; i < 256; i++)
		{
			j = Math.abs((j + S[i] + T[i]) % 256);
			temp = S[i];
			S[i] = S[j];
			S[j] = temp;
		}
	}

/**
 * @brief This method calculates the Key-Stream, produces the Ciphertext, compresses it to 4 bytes and returns it.
 * @return compressed : four byte Ciphertext
 */
	public byte[] streamGeneration(byte[] someText)
	{
		byte [] temp = null;

		// zero padding
		if((someText.length % 4) != 0)	// For the last packet
		{
			int n = someText.length;
			while(n % 4!=0) // for getting the next multiple of 4
			{
				n++;
			}

			temp = new byte [n];	// creating an array of size = length of padded sequence
			for(int i = 0; i < someText.length ; i++)
				temp[i] = someText[i]; // copying the data and header in the array

			for(int i = someText.length; i < n ; i++)
				temp[i] = 0; 		// zero padding the remaining elements in the array

		}
		else	// For all packets except the last one
		{
			temp = new byte [someText.length];  // creating an array of size = length of padded sequence
			for(int i = 0; i < someText.length ; i++)
				temp[i] = someText[i]; 			// copying the data and header in the array
		}

		// keystream 'k' generation
		byte[] cipherText = new byte[temp.length];
		byte [] compressed = new byte[4];
		int x = 0; // index required for generation
		int y = 0; //index required for generation
		int z = 0; // index for the cipherText

		while (z < temp.length)
		{
			x = Math.abs((x + 1) % 256);
			y = Math.abs((y + S[x]) % 256);
			// Swapping the elements
			byte temp1 = S[x];
			S[x] = S[y];
			S[y] = temp1;
			int t = Math.abs((S[x] + S[y]) % 256);
			byte k = S[t];	// keystream is formed: 'k'

			// Forming the ciphertext
			cipherText[z] = (byte)(temp[z] ^ k );
			z++;
		}

		// compress the cipherText stream to 4 bytes
		for(int i = 0 ; i < 4; i++)
		{
			for(int j = i; j < temp.length; j+=4)
			{
				compressed[i] ^=  temp[j];
			}
		}
		return compressed;
	} //Stream generation ends here

/**
 * @brief This method converts integer to byte array
 * @return converted: converted byte array
 */
	public byte[] intToByteArray(int integer)
	{
	    byte[] converted = new byte[4];
	    // shifting the bits towards right and extracting lower 8 bits as a byte in the byte array
	    for (int j = 0; j < 4; j++)
	    {
	        converted[3-j] = (byte)(integer >> (8*j)); // no need of logical right sift because we donot need the signed bit since seq numbers are always positive
	    }
	    return converted;
	}

/**
 * @brief This method converts a byte array in to equivalent integer value
 * @return converted: converted byte array
 */
	public int byteArrayToInt(byte byteArray[])
	{
		int integer = 0;
		for (int i = 0; i < 4; i++)
		{
	        integer = integer << 8;
	        integer = integer | (byteArray[i] & 0xff); // to make sure that we dont have any signed bits in the MSB
	    }
	    return integer;
	}

} // class ends here
