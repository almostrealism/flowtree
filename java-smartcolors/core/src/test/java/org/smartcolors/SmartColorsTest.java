package org.smartcolors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bitcoinj.core.*;
import org.junit.Before;
import org.junit.Test;
import org.smartcolors.core.SmartColors;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.smartcolors.Utils.parseBinary;

public class SmartColorsTest {
    private ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    public void testMsbdropUnpad() throws IOException {
        List<List<String>> items =
                mapper.readValue(FixtureHelpers.fixture("unpadding.json"),
                        new TypeReference<List<List<String>>>() {
                        });
        for (List<String> item : items) {
            checkUnpad(item.get(0), item.get(1));
        }
    }

    private void checkUnpad(String paddedBinaryString, String expected) {
        assertEquals(paddedBinaryString + " -> " + expected, parseBinary(expected), SmartColors.removeMsbdropValuePadding(parseBinary(paddedBinaryString)));
    }

    @Test
    public void testMsbdropPad() throws IOException {
        List<List<String>> items =
                mapper.readValue(FixtureHelpers.fixture("padding.json"),
                        new TypeReference<List<List<String>>>() {
                        });
        for (List<String> item : items) {
            checkPad(item.get(0), item.get(1), item.get(2));
        }
    }

    @Test
    public void testAddress() throws AddressFormatException {
        NetworkParameters params = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
        Address address = new ECKey().toAddress(params);
        Address assetAddress = SmartColors.toAssetAddress(address, true);
        assertEquals("S", assetAddress.toString().substring(0, 1));
        Address address1 = SmartColors.fromAssetAddress(assetAddress, params);
        assertArrayEquals(address.getHash160(), address1.getHash160());
        assertArrayEquals(assetAddress.getHash160(), new Address(SmartColors.getAssetParameters(true), assetAddress.toString()).getHash160());
        assertEquals(params, address1.getParameters());
        assertTrue(assetAddress.isP2SHAddress());
    }

    @Test
    public void testAddressTestnet() throws AddressFormatException {
        NetworkParameters params = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        Address address = new ECKey().toAddress(params);
        Address assetAddress = SmartColors.toAssetAddress(address, false);
        assertEquals("Z", assetAddress.toString().substring(0, 1));
        Address address1 = SmartColors.fromAssetAddress(assetAddress, params);
        assertArrayEquals(address.getHash160(), address1.getHash160());
        assertArrayEquals(assetAddress.getHash160(), new Address(SmartColors.getAssetParameters(false), assetAddress.toString()).getHash160());
        assertEquals(params, address1.getParameters());
        assertTrue(assetAddress.isP2SHAddress());
    }

    private void checkPad(String binaryString, String binaryMinimum, String expected) {
        assertEquals(binaryString + " -> " + expected + " min " + binaryMinimum,
                parseBinary(expected),
                SmartColors.addMsbdropValuePadding(parseBinary(binaryString), parseBinary(binaryMinimum)));
    }
}
