HTTP_PORT = 9999
HTTP_ROOT = '/Users/kanshan/gitProjects/Coder'

def server = new ServerSocket(HTTP_PORT)
while(true){
    def socket = server.accept()
    new Thread({
        def input = socket.inputStream
        def output = socket.outputStream
        try{
            def outMessage = handler(parserRequest(input))
            output.write(outMessage.getBytes())
        }catch(Exception ex){
            println ex
        }finally{
            input.close()
            output.flush()
            output.close()
        }
    } as Runnable).start()
}
def parserRequest(InputStream input){
    def reader = input.newReader()
    def protocol = []
    while(reader.ready()){
        def line = reader.readLine()
        if(!line.isBlank()) protocol << line
    }
    def firstLine = protocol[0].split(' ')
    def wrapper = [:]
    wrapper.method = firstLine[0]
    wrapper.router = firstLine[1]
    wrapper.version = firstLine[2]
    1.upto(protocol.size() - 1){
        def kv = protocol[it].split(':')
        wrapper."${kv[0]}" = kv[1].trim()
    }
    return wrapper
}
def handler(wrapper){
    def buffer = new StringBuffer()
    buffer.append("${wrapper.version} 200 OK\r\n")
    buffer.append("")
    buffer.append("\r\n")
    def file = new File("${HTTP_ROOT}${File.separator}${wrapper.router}")
    if(!file.isDirectory() && file.exists() && file.canRead()){
        buffer.append(file.text)
    }
    return buffer.toString()
}