package e93.assembler.ast;

import e93.assembler.Instruction;

public class And extends Instruction {
    @Override
    public void accept(final AssemblyVisitor assemblyVisitor) {
        assemblyVisitor.visit(this);
    }
}
