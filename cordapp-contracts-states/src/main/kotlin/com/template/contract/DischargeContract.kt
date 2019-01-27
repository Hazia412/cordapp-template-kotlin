package com.template.contract

import com.template.contract.AdmissionContract
import com.template.state.Admission
import com.template.state.Discharge
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction

class DischargeContract : Contract {
    companion object {
        val ID = "com.template.contract.DischargeContract"
        //   val DISCHARGE_DOC_HASH = ""
    }

    interface Commands : CommandData {
        class Discharge : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value){
            is Commands.Discharge -> verifyDischarge(tx, command)
        }
    }

    private fun verifyDischarge(tx: LedgerTransaction, command: CommandWithParties<DischargeContract.Commands>) {
        requireThat {
            "Transaction should have zero inputs" using (tx.inputs.isEmpty())
            "Transaction should have one output" using (tx.outputs.size == 1)
            val outputState = tx.outputStates.get(0) as Discharge
            //val outputState2 = tx.outputsOfType<InvoiceState>().get(0)
            "Admission should be signed by hospital" using (command.signers.contains(outputState.hospital.owningKey))
            "The ehr value should be positive" using (outputState.ehr > 0)
            "Transaction should have one attachment" using (tx.attachments.size == 1)

            //TODO validate hash of attachment

        }

    }
}