rootProject.name = "dagx"

include(":spi")
include(":core")

include(":extensions:protocol:web")
include(":extensions:control-http")

include(":extensions:ids:ids-spi")
include(":extensions:ids:ids-catalog-memory")
include(":extensions:ids:ids-api-catalog")

include(":runtime")
include(":client")
