package cc.etherspace

import cc.etherspace.calladapter.CallAdapter
import cc.etherspace.calladapter.PassThroughCallAdaptor
import cc.etherspace.web3j.Web3jAdapter
import okhttp3.OkHttpClient
import org.web3j.crypto.Credentials
import org.web3j.tx.Contract
import org.web3j.tx.ManagedTransaction
import java.io.IOException
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import java.math.BigInteger

class EtherSpace(private val web3: Web3jAdapter,
                 private val credentials: Credentials?,
                 private val callAdapters: List<CallAdapter<Any, Any>>) {
    @Suppress("UNCHECKED_CAST")
    fun <T> create(smartContract: SolAddress, service: Class<T>): T {
        val defaultOptions = createOptionsFromAnnotation(service)
        return Proxy.newProxyInstance(service.classLoader, arrayOf(service)) { proxy, method, args ->
            invokeFunction(smartContract, method, args?.toList() ?: emptyList(), defaultOptions)
        } as T
    }

    @Throws(IOException::class)
    private fun invokeFunction(smartContract: SolAddress,
                               method: Method,
                               args: List<Any>,
                               defaultOptions: Options): Any {
        val options = (args.firstOrNull { it is Options } ?: createOptionsFromAnnotation(method,
                defaultOptions)) as Options
        val params = args.filter { it !is Options }
        val callAdapter = callAdapters.first { it.adaptable(method.genericReturnType, method.annotations) }
        val actualReturnType = callAdapter.toActualReturnType(method.genericReturnType)
        return callAdapter.adapt {
            when {
                method.getAnnotation(Call::class.java) != null -> invokeViewFunction(smartContract,
                        method.name,
                        params,
                        actualReturnType,
                        options)
                method.getAnnotation(Send::class.java) != null -> invokeTransactionFunction(smartContract,
                        method.name,
                        params,
                        actualReturnType,
                        options
                )
                else -> {
                    throw IllegalArgumentException("There is no Send/Call annotation on this method")
                }
            }
        }
    }

    private fun createOptionsFromAnnotation(annotated: AnnotatedElement,
                                            defaultOptions: Options = Options()): Options {
        val g = annotated.getAnnotation(Gas::class.java)
        return g?.let { Options(gas = g.gas.toBigInteger(), gasPrice = g.gasPrice.toBigInteger()) } ?: defaultOptions
    }

    @Throws(IOException::class)
    private fun invokeTransactionFunction(smartContract: SolAddress,
                                          functionName: String,
                                          args: List<Any>,
                                          returnType: Type,
                                          options: Options): String {
        val contractFunction = Web3.ContractFunction(functionName,
                args,
                returnType.listTupleActualTypes())
        val encodedFunction = web3.abi.encodeFunctionCall(contractFunction.args, contractFunction.name)
        val nonce = web3.eth.getTransactionCount(credentials!!.address)
        val transactionObject = Web3.TransactionObject(credentials.address,
                smartContract.address,
                encodedFunction,
                options,
                nonce)
        val transactionResponse = web3.eth.sendTransaction(transactionObject, credentials)
        if (transactionResponse.hasError()) {
            throw IOException("Error processing transaction request: " + transactionResponse.error.message)
        }
        return transactionResponse.transactionHash
    }

    @Throws(IOException::class)
    private fun invokeViewFunction(smartContract: SolAddress,
                                   functionName: String,
                                   args: List<Any>,
                                   returnType: Type,
                                   options: Options): Any {
        val contractFunction = Web3.ContractFunction(functionName,
                args,
                returnType.listTupleActualTypes())
        val encodedFunction = web3.abi.encodeFunctionCall(contractFunction.args, contractFunction.name)
        val transactionObject = Web3.TransactionObject(credentials!!.address,
                smartContract.address,
                encodedFunction,
                options)
        val transactionResponse = web3.eth.call(transactionObject)
        if (transactionResponse.hasError()) {
            throw IOException("Error processing request: " + transactionResponse.error.message)
        }
        val values = web3.abi.decodeParameters(contractFunction.returnTypes, transactionResponse.value)
        return returnType.createTupleInstance(values)
    }

    @Suppress("unused")
    class Builder {
        var provider: String = "http://localhost:8545/"

        var credentials: Credentials? = null

        var callAdapters: List<CallAdapter<Any, Any>> = mutableListOf()

        var client: OkHttpClient? = null

        fun provider(provider: String) = apply { this.provider = provider }

        fun credentials(credentials: Credentials) = apply { this.credentials = credentials }

        fun client(client: OkHttpClient) = apply { this.client = client }

        fun addCallAdapter(callAdapter: CallAdapter<Any, Any>) = apply { this.callAdapters += callAdapter }

        fun build(): EtherSpace {
            val web3 = Web3jAdapter(provider, client)
            return EtherSpace(web3,
                    credentials,
                    callAdapters + PassThroughCallAdaptor())
        }
    }

    data class Options(val value: BigInteger = BigInteger.ZERO,
                       val gas: BigInteger = Contract.GAS_LIMIT,
                       val gasPrice: BigInteger = ManagedTransaction.GAS_PRICE)

    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }
}