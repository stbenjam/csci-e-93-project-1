package e93.assembler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * A starting point for an assembler. Very bare bones.
 *
 * @author markford
 */
public class Assembler {

    /**
     * The maximum number of registers in the system. This is used to verify
     * that the assembly line doesn't refer to an impossible register value.
     */
    private static final int MAX_REGISTERS = 16;

    /**
     * A mask used to extract the register value from an encoded instruction.
     * This should be enough bits to mask all possible values of the register.
     */
    private static final int REGISTER_MASK = 0xf;

    /**
     * Parses a line of assembly and returns an instruction that the system will
     * know how to execute.
     *
     * At this point, the instruction may refer to a label that needs to be
     * resolved.
     *
     * Note: the format of the instruction is the very basic format that was
     *       outlined in section. It uses commas to separate each of the values
     *       for the instruction. This is a little verbose but very easy to parse.
     *       The most common mistake is to forget a comma somewhere.
     *
     * @param line raw line of assembly to parse into an instruction
     * @return an instruction
     * @throws IllegalArgumentException if the line cannot be parsed.
     */
    public Instruction parse(String line) {
        String[] split = line.split(",");
        if (split.length > 3) {
            throw new IllegalArgumentException("unknown instruction:" + line);
        }
        String type = split[0].trim();
        switch (type) {
            case "AND": {
                OpCode opcode = OpCode.ALU;
                int r1 = toRegister(split[1].trim());
                int r2 = toRegister(split[2].trim());
                int functionCode = 1;
                return new Instruction()
                        .setOpcode(opcode)
                        .setR1(r1)
                        .setR2(r2)
                        .setFunc(functionCode);
            }
            case "ADDI": {
                OpCode opcode = OpCode.ADDI;
                int r1 = toRegister(split[1].trim());
                int immediate = toImmediate(split[2].trim());
                return new Instruction()
                        .setOpcode(opcode)
                        .setR1(r1)
                        .setImmediate(immediate);
            }
            default:
                throw new IllegalArgumentException("unknown instruction. Are you missing a comma?: " + line);
        }
    }

    /**
     * Encodes an instruction in order to write it to a MIF file or similar.
     *
     * At this point, your assembler should have resolved any labels to numeric
     * values in order to encode the instruction as all numbers.
     *
     * @param instruction an instruction that's had its label resolved (assuming it had one)
     * @return integer version of the instruction
     */
    public int encode(Instruction instruction) {

        assert instruction.getLabel() == null;

        switch(instruction.getOpcode()) {
            case ALU:
                switch(instruction.getFunc()) {
                    /*
                        check your work here: http://www.binaryhexconverter.com/decimal-to-binary-converter
                     */
                    case ALUFunctionCodes.AND:
                        return
                                // opcode is bits 15..12
                                instruction.getOpcode().getValue()<<12 |
                                // r1 is bits 11..8
                                instruction.getR1() << 8 |
                                // r2 is bits 7..4
                                instruction.getR2() << 4 |
                                // func is bits 3..0
                                instruction.getFunc()
                                ;
                }
                break;
        }
        throw new IllegalArgumentException("unhandled instruction:" + instruction);
    }

    /**
     * Decodes an instruction from its encoded form. You'll need something like
     * this for when you write the emulator.
     *
     * @param encoded numeric form of the instruction that we'll decode
     * @return Instruction
     * @throws IllegalArgumentException if we don't know how to decode it
     */
    public Instruction decode(int encoded) {
        // the opcode is always in 15..12 but the rest of the instruction is
        // unknown until we know what the opcode is so get that first!
        int value = encoded >> 12;
        OpCode opCode = OpCode.fromEncoded(value);
        switch (opCode) {
            case ALU:
                // get the function code to figure out what type of ALU operation
                // it is. The function code is the lower two bits which I can get
                // by AND'ing the number 3 (which is 11 in binary)
                int functionCode = encoded & 0x3;
                switch(functionCode) {
                    case ALUFunctionCodes.AND:
                        // r1 is always in 11..8
                        // shift right to drop the low order bits and then mask with
                        // REGISTER_MASK in order to get all of the
                        // bits for the register number
                        int r1 = (encoded >> 8) & REGISTER_MASK;
                        // r2 is always in 7..4
                        // shift right to drop the low order bits and then mask with
                        // REGISTER_MASK in order to get all of the
                        // bits for the register number
                        int r2 = (encoded >> 4) & REGISTER_MASK;
                        return new Instruction()
                                .setOpcode(opCode)
                                .setFunc(ALUFunctionCodes.AND)
                                .setR1(r1)
                                .setR2(r2);
                }
        }
        throw new IllegalArgumentException("unhandled encoded instruction:" + encoded);
    }

    /**
     * Parses the source into a list of Instructions.
     * @param reader Source for the program
     * @return list of instructions that are ready to encode
     * @throws IOException
     */
    public List<Instruction> parse(Reader reader) throws IOException {
        List<Instruction> instructions = new ArrayList<>();
        BufferedReader br = new BufferedReader(reader);
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("#")) {
                // it's a comment, ignore it
                continue;
            }

            // todo need to handle labels here

            Instruction instruction = parse(line);
            instructions.add(instruction);
        }

        return instructions;
    }

    /**
     * Helper method that converts a reference to a register to an int
     * @param s reference to a register in the format we're expecting
     * @return number of the register which is within range
     * @throws IllegalArgumentException if we can't parse it or it's out of range
     */
    private int toRegister(String s) {
        if (!s.startsWith("$r")) {
            throw new IllegalArgumentException("unknown register format:" + s);
        }
        int register = Integer.parseInt(s.substring(2));
        if (register > MAX_REGISTERS-1) {
            throw new IllegalArgumentException("unknown register: " + register);
        }
        return register;
    }

    /**
     * Helper method that converts an immediate value to an integer
     * @param s string version of a decimal number to use as an immediate
     * @return converted integer
     * @throws NumberFormatException if an unknown format
     */
    private int toImmediate(String s) {
        // todo - you may want to support hex
        // todo - assert the register value can be encoded

        // With respect to the encoding assertion, keep in mind that you may only
        // have 8 bits for an immediate value in your instruction (more for a
        // JUMP). You may allow the programmer to use an immediate value outside
        // of this range but that requires your assembler to handle it. For
        // example, the assembler might see a large immediate value and then
        // generate a couple of instructions to handle it. It might use a
        // temporary register and LUI and similar instructions to put the large
        // immediate into the temporary register and then rewrite the instruction
        // to use this temporary register instead of an immediate.
        return Integer.parseInt(s);
    }
}