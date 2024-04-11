circuit ALU :
  module ALU :
    input clock : Clock
    input reset : UInt<1>
    output io : { flip instruction : UInt<32>, flip rj : UInt<64>, flip rk : UInt<64>, flip rd_in : UInt<64>, flip imm : UInt<64>, rd : UInt<64>}

    io.rd <= UInt<1>("h0") @[ALU.scala 29:11]

