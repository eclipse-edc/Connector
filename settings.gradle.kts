rootProject.name = "dagx"

include(":spi")
include(":core")

include(":extensions:protocol:web")
include(":extensions:control-http")

include(":extensions:transfer:transfer-core")
include(":extensions:transfer:transfer-nifi")

include(":extensions:ids:ids-spi")
include(":extensions:ids:ids-core")
include(":extensions:ids:ids-catalog-memory")
include(":extensions:ids:ids-api-catalog")
include(":extensions:ids:ids-api-transfer")

include(":runtime")
include(":client")
