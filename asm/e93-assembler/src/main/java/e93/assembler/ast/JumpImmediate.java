package e93.assembler.ast;

import e93.assembler.Instruction;

public class JumpImmediate extends Instruction {
    @Override
    public void accept(final AssemblyVisitor assemblyVisitor) {
        assemblyVisitor.visit(this);
    }
}
