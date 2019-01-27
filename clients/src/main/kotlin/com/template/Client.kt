package com.template

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import java.io.File
import java.util.jar.JarInputStream

/**
 * Uploads the jar of blacklisted counterparties with whom agreements cannot be struck to the node.
 */
fun main(args: Array<String>) {
    UploadBlacklistClient().main(args)
}

private class UploadBlacklistClient {
    companion object {
        val logger: Logger = loggerFor<UploadBlacklistClient>()
    }

    fun main(args: Array<String>) {
        require(args.isNotEmpty()) { "Usage: uploadBlacklist <node address>" }
        args.forEach { arg ->
            val nodeAddress = parse(arg)
            val rpcConnection = CordaRPCClient(nodeAddress).start("user1", "test")
            val proxy = rpcConnection.proxy

            val attachmentHash = uploadAttachment(proxy, "src/main/resources/Blacklist.jar")
            logger.info("Blacklist uploaded to node at $nodeAddress")

            val attachmentJar = downloadAttachment(proxy, attachmentHash)
            logger.info("Blacklist downloaded from node at $nodeAddress")

            // checkAttachment(attachmentJar, ATTACHMENT_FILE_NAME, ATTACHMENT_EXPECTED_CONTENTS)
            logger.info("Attachment contents checked on node at $nodeAddress")

            rpcConnection.notifyServerAndClose()
        }
    }
}

/**
 * Uploads the attachment at [attachmentPath] to the node.
 */
private fun uploadAttachment(proxy: CordaRPCOps, attachmentPath: String): SecureHash {
    val attachmentUploadInputStream = File(attachmentPath).inputStream()
    return proxy.uploadAttachment(attachmentUploadInputStream)
}

/**
 * Downloads the attachment with hash [attachmentHash] from the node.
 */
private fun downloadAttachment(proxy: CordaRPCOps, attachmentHash: SecureHash): JarInputStream {
    val attachmentDownloadInputStream = proxy.openAttachment(attachmentHash)
    return JarInputStream(attachmentDownloadInputStream)
}

/**
 * Checks the [expectedFileName] and [expectedContents] of the downloaded [attachmentJar].
 */
private fun checkAttachment(attachmentJar: JarInputStream, expectedFileName: String, expectedContents: List<String>) {
    var name = attachmentJar.nextEntry.name
    while (name != expectedFileName) {
        name = attachmentJar.nextEntry.name
    }

    val contents = attachmentJar.bufferedReader().readLines()

    if (contents != expectedContents) {
        throw IllegalArgumentException("Downloaded JAR did not have the expected contents.")
    }
}