import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.Charset

HTTP_PORT = 9999
HTTP_ROOT = '/Users/kanshan/work/gitProjects/Coder'

def channel = ServerSocketChannel.open()
def selector = Selector.open()

channel.bind(new InetSocketAddress(HTTP_PORT))
channel.configureBlocking(false)
channel.register(selector, SelectionKey.OP_ACCEPT)
while(true){
    if(selector.select(1000) == 0) continue

    def keys = selector.selectedKeys().iterator()
    while(keys.hasNext()){
        def key = keys.next()
        def socket = ((ServerSocketChannel) key.channel()).accept()
        if(key.isAcceptable()){
            def byteBuffer = ByteBuffer.allocate(1024 * 1024)
            def html = handler(parserRequest(socket))
            byteBuffer.put(html.getBytes())
            byteBuffer.flip()
            while(byteBuffer.hasRemaining()){
                socket.write(byteBuffer)
            }
            byteBuffer.clear()
        }
        socket.close()
        keys.remove()
    }
}
def parserRequest(SocketChannel socket){
    def protocol = []

    ByteBuffer byteBuffer = ByteBuffer.allocate(1024)
    while(socket.read(byteBuffer) != -1){
        byteBuffer.flip()
        protocol = new String(byteBuffer.array()).split("\r\n")
        byteBuffer.clear()
        break
    }
    if(protocol){
        def firstLine = protocol[0].split(' ')
        def wrapper = [:]
        wrapper.method = firstLine[0]
        wrapper.router = firstLine[1]
        wrapper.version = firstLine[2]
        1.upto(protocol.size() - 1){
            def kv = protocol[it].split(':')
            if(kv.size() == 2){
                wrapper."${kv[0]}" = kv[1].trim()
            }
        }
        return wrapper
    }
}
def handler(wrapper){
    def buffer = new StringBuffer()
    if(wrapper){
        buffer.append("${wrapper.version} 200 OK\r\n")
        buffer.append("")
        buffer.append("\r\n")
        def file = new File("${HTTP_ROOT}${File.separator}${wrapper.router}")
        if(!file.isDirectory() && file.exists() && file.canRead()){
            def fileInput = new FileInputStream(file)
            def channel = fileInput.getChannel()
            def byteBuffer = ByteBuffer.allocate(1024)
            while(channel.read(byteBuffer) != -1){
                byteBuffer.flip()
                def charBuffer = Charset.forName("UTF-8").decode(byteBuffer)
                buffer.append(charBuffer.flip().array())
                byteBuffer.clear()
                charBuffer.clear()
            }
        }
    }
    return buffer.toString()
}
