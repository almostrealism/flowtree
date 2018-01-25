Welcome to _SmartColors_, a improved color kernel allowing:

* Untrusted asset servers serving SPV proofs
* Atomic swaps

# Prerequisites

```
    git clone https://github.com/devrandom/bitcoinj
    cd bitcoinj
    mvn install
```

# Compile

Clone this repo, and compile:

`mvn package`

# Try things out

```
    cd tools
    # Create wallet from seed and scan
    bin/colortool --mnemonic "click uniform area entire lamp engine sure enjoy all adult pigeon size" scan
    # Dump existing wallet, and show deposit address
    bin/colortool dump
    # Scan existing wallet
    bin/colortool scan
```

Asset definitions are in `oil.smartcolor` and `gold.smartcolor` in the `tools` module.

# Issuance

After scanning a wallet that has a Bitcoin balance:

```
    bin/colortool issue NAME 1000000
```

This will create two files: `NAME.smartcolor` and `NAME.scdef`.  The `smartcolor` JSON file is a bare-bones asset definition with
just one item of metadata - the name.  You can add other metadata, such as divisibility, as needed.  The `scdef` file
contains the consensus definition and can be imported into the SmartColors server.

Wait for the issuance transaction to confirm before proceeding.

## Adding to server

```
    # use --testnet for TestNet3
    ./smartassetctl addcolordef NAME.scdef
    ./smartassetctl scan
```

## Adding to clients

The augmented smartcolor JSON file should be added to the resources directory in this tools module in `src/main/resources/assets`.
For now also add to SmartTools.addBuiltins().

In the SmartWallet project add to `wallet/assets/smartassets`.  For existing wallets, you have to clear data and restore from seed.

The JSON file can also be put up on a web server to let users manually add.

## Recompile and rescan

From the root directory of this repo:

```
    mvn clean package
    cd tools
    bin/colortool --mnemonic "click uniform area entire lamp engine sure enjoy all adult pigeon size" scan
    bin/colortool dump
```

You should see the new asset in the balances.
