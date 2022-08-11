window.swaggerSpec={
  "openapi" : "3.0.1",
  "info" : {
    "title" : "EDC REST API",
    "description" : "All files merged by open api merger",
    "license" : {
      "name" : "Apache License v2.0",
      "url" : "http://apache.org/v2"
    },
    "version" : "1.0.0-SNAPSHOT"
  },
  "servers" : [ {
    "url" : "/"
  } ],
  "tags" : [ {
    "name" : "Data Plane control API",
    "description" : "Api targeted by the Control Plane to delegate a data transfer (Provider Push or Streaming) to the Data Plane after the contract has been successfully negotiated and agreed between the two participants. "
  }, {
    "name" : "Data Plane public API",
    "description" : "The public API of the Data Plane is a data proxy enabling a data consumer to actively querydata from the provider data source (e.g. backend Rest API, internal database...) through its Data Planeinstance. Thus the Data Plane is the only entry/output door for the data, which avoids the provider to exposedirectly its data externally.The Data Plane public API being a proxy, it supports all verbs (i.e. GET, POST, PUT, PATCH, DELETE), whichcan then conveyed until the data source is required. This is especially useful when the actual data sourceis a Rest API itself.In the same manner, any set of arbitrary query parameters, path parameters and request body are supported (in the limits fixed by the HTTP server) and can also conveyed to the actual data source."
  } ],
  "paths" : {
    "/assets" : {
      "get" : {
        "tags" : [ "Asset" ],
        "description" : "Gets all assets according to a particular query",
        "operationId" : "getAllAssets",
        "parameters" : [ {
          "name" : "offset",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "integer",
            "format" : "int32"
          }
        }, {
          "name" : "limit",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "integer",
            "format" : "int32"
          }
        }, {
          "name" : "filter",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        }, {
          "name" : "sort",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string",
            "enum" : [ "ASC", "DESC" ]
          }
        }, {
          "name" : "sortField",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/AssetResponseDto"
                  }
                }
              }
            }
          },
          "400" : {
            "description" : "Request body was malformed",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      },
      "post" : {
        "tags" : [ "Asset" ],
        "description" : "Creates a new asset together with a data address",
        "operationId" : "createAsset",
        "requestBody" : {
          "content" : {
            "application/json" : {
              "schema" : {
                "$ref" : "#/components/schemas/AssetEntryDto"
              }
            }
          }
        },
        "responses" : {
          "200" : {
            "description" : "Asset was created successfully"
          },
          "400" : {
            "description" : "Request body was malformed",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "409" : {
            "description" : "Could not create asset, because an asset with that ID already exists",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/assets/{id}" : {
      "get" : {
        "tags" : [ "Asset" ],
        "description" : "Gets an asset with the given ID",
        "operationId" : "getAsset",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "The asset",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/AssetResponseDto"
                }
              }
            }
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "An asset with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      },
      "delete" : {
        "tags" : [ "Asset" ],
        "description" : "Removes an asset with the given ID if possible. Deleting an asset is only possible if that asset is not yet referenced by a contract agreement, in which case an error is returned. DANGER ZONE: Note that deleting assets can have unexpected results, especially for contract offers that have been sent out or ongoing or contract negotiations.",
        "operationId" : "removeAsset",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "Asset was deleted successfully"
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "An asset with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "409" : {
            "description" : "The asset cannot be deleted, because it is referenced by a contract agreement",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/catalog" : {
      "get" : {
        "tags" : [ "Catalog" ],
        "operationId" : "getCatalog",
        "parameters" : [ {
          "name" : "providerUrl",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        }, {
          "name" : "offset",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "integer",
            "format" : "int32"
          }
        }, {
          "name" : "limit",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "integer",
            "format" : "int32"
          }
        }, {
          "name" : "filter",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        }, {
          "name" : "sort",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string",
            "enum" : [ "ASC", "DESC" ]
          }
        }, {
          "name" : "sortField",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "default" : {
            "description" : "Gets contract offers (=catalog) of a single connector",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/Catalog"
                }
              }
            }
          }
        }
      }
    },
    "/check/health" : {
      "get" : {
        "tags" : [ "Application Observability" ],
        "description" : "Performs a liveness probe to determine whether the runtime is working properly.",
        "operationId" : "checkHealth",
        "responses" : {
          "200" : {
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/HealthStatus"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/check/liveness" : {
      "get" : {
        "tags" : [ "Application Observability" ],
        "description" : "Performs a liveness probe to determine whether the runtime is working properly.",
        "operationId" : "getLiveness",
        "responses" : {
          "200" : {
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/HealthStatus"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/check/readiness" : {
      "get" : {
        "tags" : [ "Application Observability" ],
        "description" : "Performs a readiness probe to determine whether the runtime is able to accept requests.",
        "operationId" : "getReadiness",
        "responses" : {
          "200" : {
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/HealthStatus"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/check/startup" : {
      "get" : {
        "tags" : [ "Application Observability" ],
        "description" : "Performs a startup probe to determine whether the runtime has completed startup.",
        "operationId" : "getStartup",
        "responses" : {
          "200" : {
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/HealthStatus"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/instances" : {
      "get" : {
        "tags" : [ "Dataplane Selector" ],
        "operationId" : "getAll",
        "responses" : {
          "default" : {
            "description" : "default response",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/DataPlaneInstance"
                  }
                }
              }
            }
          }
        }
      },
      "post" : {
        "tags" : [ "Dataplane Selector" ],
        "operationId" : "addEntry",
        "requestBody" : {
          "content" : {
            "application/json" : {
              "schema" : {
                "$ref" : "#/components/schemas/DataPlaneInstance"
              }
            }
          }
        },
        "responses" : {
          "default" : {
            "description" : "default response",
            "content" : {
              "application/json" : { }
            }
          }
        }
      }
    },
    "/instances/select" : {
      "post" : {
        "tags" : [ "Dataplane Selector" ],
        "operationId" : "find",
        "requestBody" : {
          "content" : {
            "application/json" : {
              "schema" : {
                "$ref" : "#/components/schemas/SelectionRequest"
              }
            }
          }
        },
        "responses" : {
          "default" : {
            "description" : "default response",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/DataPlaneInstance"
                }
              }
            }
          }
        }
      }
    },
    "/contractnegotiations" : {
      "get" : {
        "tags" : [ "Contract Negotiation" ],
        "description" : "Returns all contract negotiations according to a query",
        "operationId" : "getNegotiations",
        "parameters" : [ {
          "name" : "offset",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "integer",
            "format" : "int32"
          }
        }, {
          "name" : "limit",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "integer",
            "format" : "int32"
          }
        }, {
          "name" : "filter",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        }, {
          "name" : "sort",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string",
            "enum" : [ "ASC", "DESC" ]
          }
        }, {
          "name" : "sortField",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ContractNegotiationDto"
                  }
                }
              }
            }
          },
          "400" : {
            "description" : "Request was malformed",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      },
      "post" : {
        "tags" : [ "Contract Negotiation" ],
        "description" : "Initiates a contract negotiation for a given offer and with the given counter part. Please note that successfully invoking this endpoint only means that the negotiation was initiated. Clients must poll the /{id}/state endpoint to track the state",
        "operationId" : "initiateContractNegotiation",
        "requestBody" : {
          "content" : {
            "application/json" : {
              "schema" : {
                "$ref" : "#/components/schemas/NegotiationInitiateRequestDto"
              }
            }
          }
        },
        "responses" : {
          "200" : {
            "description" : "The negotiation was successfully initiated. Returns the contract negotiation ID",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/NegotiationId"
                }
              }
            },
            "links" : {
              "poll-state" : {
                "operationId" : "getNegotiationState",
                "parameters" : {
                  "id" : "$response.body#/id"
                }
              }
            }
          },
          "400" : {
            "description" : "Request body was malformed",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/contractnegotiations/{id}" : {
      "get" : {
        "tags" : [ "Contract Negotiation" ],
        "description" : "Gets an contract negotiation with the given ID",
        "operationId" : "getNegotiation",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "The contract negotiation",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/ContractNegotiationDto"
                }
              }
            }
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "An contract negotiation with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/contractnegotiations/{id}/agreement" : {
      "get" : {
        "tags" : [ "Contract Negotiation" ],
        "description" : "Gets a contract agreement for a contract negotiation with the given ID",
        "operationId" : "getAgreementForNegotiation",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "The contract agreement that is attached to the negotiation, or null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/ContractNegotiationDto"
                }
              }
            }
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "An contract negotiation with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/contractnegotiations/{id}/cancel" : {
      "post" : {
        "tags" : [ "Contract Negotiation" ],
        "description" : "Requests aborting the contract negotiation. Due to the asynchronous nature of contract negotiations, a successful response only indicates that the request was successfully received. Clients must poll the /{id}/state endpoint to track the state.",
        "operationId" : "cancelNegotiation",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "Request to cancel the Contract negotiation was successfully received",
            "links" : {
              "poll-state" : {
                "operationId" : "getNegotiationState"
              }
            }
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "A contract negotiation with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/contractnegotiations/{id}/decline" : {
      "post" : {
        "tags" : [ "Contract Negotiation" ],
        "description" : "Requests cancelling the contract negotiation. Due to the asynchronous nature of contract negotiations, a successful response only indicates that the request was successfully received. Clients must poll the /{id}/state endpoint to track the state.",
        "operationId" : "declineNegotiation",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "Request to decline the Contract negotiation was successfully received",
            "links" : {
              "poll-state" : {
                "operationId" : "getNegotiationState"
              }
            }
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "A contract negotiation with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/contractnegotiations/{id}/state" : {
      "get" : {
        "tags" : [ "Contract Negotiation" ],
        "description" : "Gets the state of a contract negotiation with the given ID",
        "operationId" : "getNegotiationState",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "The contract negotiation's state",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/NegotiationState"
                }
              }
            }
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "An contract negotiation with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/token" : {
      "get" : {
        "tags" : [ "Token Validation" ],
        "description" : "Checks that the provided token has been signed by the present entity and asserts its validity. If token is valid, then the data address contained in its claims is decrypted and returned back to the caller.",
        "operationId" : "validate",
        "parameters" : [ {
          "name" : "Authorization",
          "in" : "header",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "Token is valid"
          },
          "400" : {
            "description" : "Request was malformed"
          },
          "403" : {
            "description" : "Token is invalid"
          }
        }
      }
    },
    "/callback/{processId}/deprovision" : {
      "post" : {
        "tags" : [ "HTTP Provisioner Webhook" ],
        "operationId" : "callDeprovisionWebhook",
        "parameters" : [ {
          "name" : "processId",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "requestBody" : {
          "content" : {
            "application/json" : {
              "schema" : {
                "$ref" : "#/components/schemas/DeprovisionedResource"
              }
            }
          }
        },
        "responses" : {
          "default" : {
            "description" : "default response",
            "content" : {
              "application/json" : { }
            }
          }
        }
      }
    },
    "/callback/{processId}/provision" : {
      "post" : {
        "tags" : [ "HTTP Provisioner Webhook" ],
        "operationId" : "callProvisionWebhook",
        "parameters" : [ {
          "name" : "processId",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "requestBody" : {
          "content" : {
            "application/json" : {
              "schema" : {
                "$ref" : "#/components/schemas/ProvisionerWebhookRequest"
              }
            }
          }
        },
        "responses" : {
          "default" : {
            "description" : "default response",
            "content" : {
              "application/json" : { }
            }
          }
        }
      }
    },
    "/contractdefinitions" : {
      "get" : {
        "tags" : [ "Contract Definition" ],
        "description" : "Returns all contract definitions according to a query",
        "operationId" : "getAllContractDefinitions",
        "parameters" : [ {
          "name" : "offset",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "integer",
            "format" : "int32"
          }
        }, {
          "name" : "limit",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "integer",
            "format" : "int32"
          }
        }, {
          "name" : "filter",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        }, {
          "name" : "sort",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string",
            "enum" : [ "ASC", "DESC" ]
          }
        }, {
          "name" : "sortField",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ContractDefinitionResponseDto"
                  }
                }
              }
            }
          },
          "400" : {
            "description" : "Request was malformed",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      },
      "post" : {
        "tags" : [ "Contract Definition" ],
        "description" : "Creates a new contract definition",
        "operationId" : "createContractDefinition",
        "requestBody" : {
          "content" : {
            "*/*" : {
              "schema" : {
                "$ref" : "#/components/schemas/ContractDefinitionRequestDto"
              }
            }
          }
        },
        "responses" : {
          "200" : {
            "description" : "contract definition was created successfully"
          },
          "400" : {
            "description" : "Request body was malformed",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "409" : {
            "description" : "Could not create contract definition, because a contract definition with that ID already exists",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/contractdefinitions/{id}" : {
      "get" : {
        "tags" : [ "Contract Definition" ],
        "description" : "Gets an contract definition with the given ID",
        "operationId" : "getContractDefinition",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "The contract definition",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/ContractDefinitionResponseDto"
                }
              }
            }
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "An contract agreement with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      },
      "delete" : {
        "tags" : [ "Contract Definition" ],
        "description" : "Removes a contract definition with the given ID if possible. DANGER ZONE: Note that deleting contract definitions can have unexpected results, especially for contract offers that have been sent out or ongoing or contract negotiations.",
        "operationId" : "deleteContractDefinition",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "Contract definition was deleted successfully"
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "A contract definition with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/transfer" : {
      "post" : {
        "tags" : [ "Data Plane control API" ],
        "description" : "Initiates a data transfer for the given request. The transfer will be performed asynchronously.",
        "operationId" : "initiateTransfer",
        "requestBody" : {
          "content" : {
            "application/json" : {
              "schema" : {
                "$ref" : "#/components/schemas/DataFlowRequest"
              }
            }
          }
        },
        "responses" : {
          "200" : {
            "description" : "Data transfer initiated"
          },
          "400" : {
            "description" : "Failed to validate request"
          }
        }
      }
    },
    "/transfer/{processId}" : {
      "get" : {
        "tags" : [ "Data Plane control API" ],
        "description" : "Get the current state of a data transfer.",
        "operationId" : "getTransferState",
        "parameters" : [ {
          "name" : "processId",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "Missing access token"
          }
        }
      }
    },
    "/{any}" : {
      "get" : {
        "tags" : [ "Data Plane public API" ],
        "description" : "Send `GET` data query to the Data Plane.",
        "operationId" : "get",
        "responses" : {
          "400" : {
            "description" : "Missing access token"
          },
          "403" : {
            "description" : "Access token is expired or invalid"
          },
          "500" : {
            "description" : "Failed to transfer data"
          }
        }
      },
      "put" : {
        "tags" : [ "Data Plane public API" ],
        "description" : "Send `PUT` data query to the Data Plane.",
        "operationId" : "put",
        "responses" : {
          "400" : {
            "description" : "Missing access token"
          },
          "403" : {
            "description" : "Access token is expired or invalid"
          },
          "500" : {
            "description" : "Failed to transfer data"
          }
        }
      },
      "post" : {
        "tags" : [ "Data Plane public API" ],
        "description" : "Send `POST` data query to the Data Plane.",
        "operationId" : "post",
        "responses" : {
          "400" : {
            "description" : "Missing access token"
          },
          "403" : {
            "description" : "Access token is expired or invalid"
          },
          "500" : {
            "description" : "Failed to transfer data"
          }
        }
      },
      "delete" : {
        "tags" : [ "Data Plane public API" ],
        "description" : "Send `DELETE` data query to the Data Plane.",
        "operationId" : "delete",
        "responses" : {
          "400" : {
            "description" : "Missing access token"
          },
          "403" : {
            "description" : "Access token is expired or invalid"
          },
          "500" : {
            "description" : "Failed to transfer data"
          }
        }
      },
      "patch" : {
        "tags" : [ "Data Plane public API" ],
        "description" : "Send `PATCH` data query to the Data Plane.",
        "operationId" : "patch",
        "responses" : {
          "400" : {
            "description" : "Missing access token"
          },
          "403" : {
            "description" : "Access token is expired or invalid"
          },
          "500" : {
            "description" : "Failed to transfer data"
          }
        }
      }
    },
    "/federatedcatalog" : {
      "post" : {
        "operationId" : "getCachedCatalog",
        "requestBody" : {
          "content" : {
            "application/json" : {
              "schema" : {
                "$ref" : "#/components/schemas/FederatedCatalogCacheQuery"
              }
            }
          }
        },
        "responses" : {
          "default" : {
            "description" : "default response",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ContractOffer"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/policydefinitions" : {
      "get" : {
        "tags" : [ "Policy" ],
        "description" : "Returns all policy definitions according to a query",
        "operationId" : "getAllPolicies",
        "parameters" : [ {
          "name" : "offset",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "integer",
            "format" : "int32"
          }
        }, {
          "name" : "limit",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "integer",
            "format" : "int32"
          }
        }, {
          "name" : "filter",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        }, {
          "name" : "sort",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string",
            "enum" : [ "ASC", "DESC" ]
          }
        }, {
          "name" : "sortField",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/PolicyDefinition"
                  }
                }
              }
            }
          },
          "400" : {
            "description" : "Request was malformed",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      },
      "post" : {
        "tags" : [ "Policy" ],
        "description" : "Creates a new policy definition",
        "operationId" : "createPolicy",
        "requestBody" : {
          "content" : {
            "application/json" : {
              "schema" : {
                "$ref" : "#/components/schemas/PolicyDefinition"
              }
            }
          }
        },
        "responses" : {
          "200" : {
            "description" : "policy definition was created successfully"
          },
          "400" : {
            "description" : "Request body was malformed",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "409" : {
            "description" : "Could not create policy definition, because a contract definition with that ID already exists",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/policydefinitions/{id}" : {
      "get" : {
        "tags" : [ "Policy" ],
        "description" : "Gets a policy definition with the given ID",
        "operationId" : "getPolicy",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "The  policy definition",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/PolicyDefinition"
                }
              }
            }
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "An  policy definition with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      },
      "delete" : {
        "tags" : [ "Policy" ],
        "description" : "Removes a policy definition with the given ID if possible. Deleting a policy definition is only possible if that policy definition is not yet referenced by a contract definition, in which case an error is returned. DANGER ZONE: Note that deleting policy definitions can have unexpected results, do this at your own risk!",
        "operationId" : "deletePolicy",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "Policy definition was deleted successfully"
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "An policy definition with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "409" : {
            "description" : "The policy definition cannot be deleted, because it is referenced by a contract definition",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/contractagreements" : {
      "get" : {
        "tags" : [ "Contract Agreement" ],
        "description" : "Gets all contract agreements according to a particular query",
        "operationId" : "getAllAgreements",
        "parameters" : [ {
          "name" : "offset",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "integer",
            "format" : "int32"
          }
        }, {
          "name" : "limit",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "integer",
            "format" : "int32"
          }
        }, {
          "name" : "filter",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        }, {
          "name" : "sort",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string",
            "enum" : [ "ASC", "DESC" ]
          }
        }, {
          "name" : "sortField",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ContractAgreementDto"
                  }
                }
              }
            }
          },
          "400" : {
            "description" : "Request body was malformed",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/contractagreements/{id}" : {
      "get" : {
        "tags" : [ "Contract Agreement" ],
        "description" : "Gets an contract agreement with the given ID",
        "operationId" : "getContractAgreement",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "The contract agreement",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/ContractAgreementDto"
                }
              }
            }
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "An contract agreement with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/transferprocess" : {
      "get" : {
        "tags" : [ "Transfer Process" ],
        "description" : "Returns all transfer process according to a query",
        "operationId" : "getAllTransferProcesses",
        "parameters" : [ {
          "name" : "offset",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "integer",
            "format" : "int32"
          }
        }, {
          "name" : "limit",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "integer",
            "format" : "int32"
          }
        }, {
          "name" : "filter",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        }, {
          "name" : "sort",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string",
            "enum" : [ "ASC", "DESC" ]
          }
        }, {
          "name" : "sortField",
          "in" : "query",
          "required" : false,
          "style" : "form",
          "explode" : true,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/TransferProcessDto"
                  }
                }
              }
            }
          },
          "400" : {
            "description" : "Request was malformed",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      },
      "post" : {
        "tags" : [ "Transfer Process" ],
        "description" : "Initiates a data transfer with the given parameters. Please note that successfully invoking this endpoint only means that the transfer was initiated. Clients must poll the /{id}/state endpoint to track the state",
        "operationId" : "initiateTransfer",
        "requestBody" : {
          "content" : {
            "application/json" : {
              "schema" : {
                "$ref" : "#/components/schemas/TransferRequestDto"
              }
            }
          }
        },
        "responses" : {
          "200" : {
            "description" : "The transfer was successfully initiated. Returns the transfer process ID",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/TransferId"
                }
              }
            },
            "links" : {
              "poll-state" : {
                "operationId" : "getTransferProcessState",
                "parameters" : {
                  "id" : "$response.body#/id"
                }
              }
            }
          },
          "400" : {
            "description" : "Request body was malformed",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/transferprocess/{id}" : {
      "get" : {
        "tags" : [ "Transfer Process" ],
        "description" : "Gets an transfer process with the given ID",
        "operationId" : "getTransferProcess",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "The transfer process",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/TransferProcessDto"
                }
              }
            }
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "A transfer process with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/transferprocess/{id}/cancel" : {
      "post" : {
        "tags" : [ "Transfer Process" ],
        "description" : "Requests aborting the transfer process. Due to the asynchronous nature of transfers, a successful response only indicates that the request was successfully received. Clients must poll the /{id}/state endpoint to track the state.",
        "operationId" : "cancelTransferProcess",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "Request to cancel the transfer process was successfully received",
            "links" : {
              "poll-state" : {
                "operationId" : "getTransferProcessState"
              }
            }
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "A contract negotiation with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/transferprocess/{id}/deprovision" : {
      "post" : {
        "tags" : [ "Transfer Process" ],
        "description" : "Requests the deprovisioning of resources associated with a transfer process. Due to the asynchronous nature of transfers, a successful response only indicates that the request was successfully received. This may take a long time, so clients must poll the /{id}/state endpoint to track the state.",
        "operationId" : "deprovisionTransferProcess",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "Request to deprovision the transfer process was successfully received",
            "links" : {
              "poll-state" : {
                "operationId" : "getTransferProcessState"
              }
            }
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "A contract negotiation with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    },
    "/transferprocess/{id}/state" : {
      "get" : {
        "tags" : [ "Transfer Process" ],
        "description" : "Gets the state of a transfer process with the given ID",
        "operationId" : "getTransferProcessState",
        "parameters" : [ {
          "name" : "id",
          "in" : "path",
          "required" : true,
          "style" : "simple",
          "explode" : false,
          "schema" : {
            "type" : "string"
          }
        } ],
        "responses" : {
          "200" : {
            "description" : "The  transfer process's state",
            "content" : {
              "application/json" : {
                "schema" : {
                  "$ref" : "#/components/schemas/TransferState"
                }
              }
            }
          },
          "400" : {
            "description" : "Request was malformed, e.g. id was null",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          },
          "404" : {
            "description" : "An  transfer process with the given ID does not exist",
            "content" : {
              "application/json" : {
                "schema" : {
                  "type" : "array",
                  "items" : {
                    "$ref" : "#/components/schemas/ApiErrorDetail"
                  }
                }
              }
            }
          }
        }
      }
    }
  },
  "components" : {
    "schemas" : {
      "Action" : {
        "type" : "object",
        "properties" : {
          "constraint" : {
            "$ref" : "#/components/schemas/Constraint"
          },
          "includedIn" : {
            "type" : "string"
          },
          "type" : {
            "type" : "string"
          }
        }
      },
      "ApiErrorDetail" : {
        "type" : "object",
        "properties" : {
          "invalidValue" : {
            "type" : "string"
          },
          "message" : {
            "type" : "string"
          },
          "path" : {
            "type" : "string"
          },
          "type" : {
            "type" : "string"
          }
        }
      },
      "Asset" : {
        "type" : "object",
        "properties" : {
          "createdAt" : {
            "type" : "integer",
            "format" : "int64"
          },
          "id" : {
            "type" : "string"
          },
          "properties" : {
            "type" : "object",
            "additionalProperties" : {
              "type" : "object"
            }
          }
        }
      },
      "AssetEntryDto" : {
        "required" : [ "asset", "dataAddress" ],
        "type" : "object",
        "properties" : {
          "asset" : {
            "$ref" : "#/components/schemas/AssetRequestDto"
          },
          "dataAddress" : {
            "$ref" : "#/components/schemas/DataAddressDto"
          }
        }
      },
      "AssetRequestDto" : {
        "required" : [ "properties" ],
        "type" : "object",
        "properties" : {
          "id" : {
            "type" : "string"
          },
          "properties" : {
            "type" : "object",
            "additionalProperties" : {
              "type" : "object"
            }
          }
        }
      },
      "AssetResponseDto" : {
        "type" : "object",
        "properties" : {
          "createdAt" : {
            "type" : "integer",
            "format" : "int64"
          },
          "id" : {
            "type" : "string"
          },
          "properties" : {
            "type" : "object",
            "additionalProperties" : {
              "type" : "object"
            }
          }
        }
      },
      "Catalog" : {
        "type" : "object",
        "properties" : {
          "contractOffers" : {
            "type" : "array",
            "items" : {
              "$ref" : "#/components/schemas/ContractOffer"
            }
          },
          "id" : {
            "type" : "string"
          }
        }
      },
      "Constraint" : {
        "required" : [ "edctype" ],
        "type" : "object",
        "properties" : {
          "edctype" : {
            "type" : "string"
          }
        },
        "discriminator" : {
          "propertyName" : "edctype"
        }
      },
      "ContractAgreementDto" : {
        "required" : [ "assetId", "consumerAgentId", "id", "policy", "providerAgentId" ],
        "type" : "object",
        "properties" : {
          "assetId" : {
            "type" : "string"
          },
          "consumerAgentId" : {
            "type" : "string"
          },
          "contractEndDate" : {
            "type" : "integer",
            "format" : "int64"
          },
          "contractSigningDate" : {
            "type" : "integer",
            "format" : "int64"
          },
          "contractStartDate" : {
            "type" : "integer",
            "format" : "int64"
          },
          "id" : {
            "type" : "string"
          },
          "policy" : {
            "$ref" : "#/components/schemas/Policy"
          },
          "providerAgentId" : {
            "type" : "string"
          }
        }
      },
      "ContractDefinitionRequestDto" : {
        "required" : [ "accessPolicyId", "contractPolicyId", "criteria" ],
        "type" : "object",
        "properties" : {
          "accessPolicyId" : {
            "type" : "string"
          },
          "contractPolicyId" : {
            "type" : "string"
          },
          "criteria" : {
            "type" : "array",
            "items" : {
              "$ref" : "#/components/schemas/Criterion"
            }
          },
          "id" : {
            "type" : "string"
          }
        }
      },
      "ContractDefinitionResponseDto" : {
        "type" : "object",
        "properties" : {
          "accessPolicyId" : {
            "type" : "string"
          },
          "contractPolicyId" : {
            "type" : "string"
          },
          "createdAt" : {
            "type" : "integer",
            "format" : "int64"
          },
          "criteria" : {
            "type" : "array",
            "items" : {
              "$ref" : "#/components/schemas/Criterion"
            }
          },
          "id" : {
            "type" : "string"
          }
        }
      },
      "ContractNegotiationDto" : {
        "type" : "object",
        "properties" : {
          "contractAgreementId" : {
            "type" : "string"
          },
          "counterPartyAddress" : {
            "type" : "string"
          },
          "createdAt" : {
            "type" : "integer",
            "format" : "int64"
          },
          "errorDetail" : {
            "type" : "string"
          },
          "id" : {
            "type" : "string"
          },
          "protocol" : {
            "type" : "string"
          },
          "state" : {
            "type" : "string"
          },
          "type" : {
            "type" : "string",
            "enum" : [ "CONSUMER", "PROVIDER" ]
          },
          "updatedAt" : {
            "type" : "integer",
            "format" : "int64"
          }
        }
      },
      "ContractOffer" : {
        "type" : "object",
        "properties" : {
          "asset" : {
            "$ref" : "#/components/schemas/Asset"
          },
          "assetId" : {
            "type" : "string"
          },
          "consumer" : {
            "type" : "string",
            "format" : "uri"
          },
          "contractEnd" : {
            "type" : "string",
            "format" : "date-time"
          },
          "contractStart" : {
            "type" : "string",
            "format" : "date-time"
          },
          "id" : {
            "type" : "string"
          },
          "offerEnd" : {
            "type" : "string",
            "format" : "date-time"
          },
          "offerStart" : {
            "type" : "string",
            "format" : "date-time"
          },
          "policy" : {
            "$ref" : "#/components/schemas/Policy"
          },
          "provider" : {
            "type" : "string",
            "format" : "uri"
          }
        }
      },
      "ContractOfferDescription" : {
        "required" : [ "assetId", "offerId", "policy" ],
        "type" : "object",
        "properties" : {
          "assetId" : {
            "type" : "string"
          },
          "offerId" : {
            "type" : "string"
          },
          "policy" : {
            "$ref" : "#/components/schemas/Policy"
          }
        }
      },
      "Criterion" : {
        "type" : "object",
        "properties" : {
          "operandLeft" : {
            "type" : "object"
          },
          "operandRight" : {
            "type" : "object"
          },
          "operator" : {
            "type" : "string"
          }
        }
      },
      "DataAddress" : {
        "type" : "object",
        "properties" : {
          "properties" : {
            "type" : "object",
            "additionalProperties" : {
              "type" : "string"
            }
          }
        }
      },
      "DataAddressDto" : {
        "required" : [ "properties" ],
        "type" : "object",
        "properties" : {
          "properties" : {
            "type" : "object",
            "additionalProperties" : {
              "type" : "string"
            }
          }
        }
      },
      "DataAddressInformationDto" : {
        "type" : "object",
        "properties" : {
          "properties" : {
            "type" : "object",
            "additionalProperties" : {
              "type" : "string"
            }
          }
        }
      },
      "DataFlowRequest" : {
        "type" : "object",
        "properties" : {
          "destinationDataAddress" : {
            "$ref" : "#/components/schemas/DataAddress"
          },
          "id" : {
            "type" : "string"
          },
          "processId" : {
            "type" : "string"
          },
          "properties" : {
            "type" : "object",
            "additionalProperties" : {
              "type" : "string"
            }
          },
          "sourceDataAddress" : {
            "$ref" : "#/components/schemas/DataAddress"
          },
          "traceContext" : {
            "type" : "object",
            "additionalProperties" : {
              "type" : "string"
            }
          },
          "trackable" : {
            "type" : "boolean"
          }
        }
      },
      "DataPlaneInstance" : {
        "type" : "object",
        "properties" : {
          "id" : {
            "type" : "string"
          },
          "lastActive" : {
            "type" : "integer",
            "format" : "int64"
          },
          "properties" : {
            "type" : "object",
            "additionalProperties" : {
              "type" : "object"
            }
          },
          "turnCount" : {
            "type" : "integer",
            "format" : "int32"
          },
          "url" : {
            "type" : "string",
            "format" : "url"
          }
        }
      },
      "DataRequestDto" : {
        "type" : "object",
        "properties" : {
          "assetId" : {
            "type" : "string"
          },
          "connectorId" : {
            "type" : "string"
          },
          "contractId" : {
            "type" : "string"
          }
        }
      },
      "DeprovisionedResource" : {
        "type" : "object",
        "properties" : {
          "error" : {
            "type" : "boolean"
          },
          "errorMessage" : {
            "type" : "string"
          },
          "inProcess" : {
            "type" : "boolean"
          },
          "provisionedResourceId" : {
            "type" : "string"
          }
        }
      },
      "Duty" : {
        "type" : "object",
        "properties" : {
          "action" : {
            "$ref" : "#/components/schemas/Action"
          },
          "assignee" : {
            "type" : "string"
          },
          "assigner" : {
            "type" : "string"
          },
          "consequence" : {
            "$ref" : "#/components/schemas/Duty"
          },
          "constraints" : {
            "type" : "array",
            "items" : {
              "$ref" : "#/components/schemas/Constraint"
            }
          },
          "parentPermission" : {
            "$ref" : "#/components/schemas/Permission"
          },
          "target" : {
            "type" : "string"
          },
          "uid" : {
            "type" : "string"
          }
        }
      },
      "Failure" : {
        "type" : "object",
        "properties" : {
          "messages" : {
            "type" : "array",
            "items" : {
              "type" : "string"
            }
          }
        }
      },
      "FederatedCatalogCacheQuery" : {
        "type" : "object",
        "properties" : {
          "criteria" : {
            "type" : "array",
            "items" : {
              "$ref" : "#/components/schemas/Criterion"
            }
          }
        }
      },
      "HealthCheckResult" : {
        "type" : "object",
        "properties" : {
          "component" : {
            "type" : "string"
          },
          "failure" : {
            "$ref" : "#/components/schemas/Failure"
          },
          "isHealthy" : {
            "type" : "boolean"
          }
        }
      },
      "HealthStatus" : {
        "type" : "object",
        "properties" : {
          "componentResults" : {
            "type" : "array",
            "items" : {
              "$ref" : "#/components/schemas/HealthCheckResult"
            }
          },
          "isSystemHealthy" : {
            "type" : "boolean"
          }
        }
      },
      "NegotiationId" : {
        "type" : "object",
        "properties" : {
          "id" : {
            "type" : "string"
          }
        }
      },
      "NegotiationInitiateRequestDto" : {
        "required" : [ "connectorAddress", "connectorId", "offer", "protocol" ],
        "type" : "object",
        "properties" : {
          "connectorAddress" : {
            "type" : "string"
          },
          "connectorId" : {
            "type" : "string"
          },
          "offer" : {
            "$ref" : "#/components/schemas/ContractOfferDescription"
          },
          "protocol" : {
            "type" : "string"
          }
        }
      },
      "NegotiationState" : {
        "type" : "object",
        "properties" : {
          "state" : {
            "type" : "string"
          }
        }
      },
      "Permission" : {
        "type" : "object",
        "properties" : {
          "action" : {
            "$ref" : "#/components/schemas/Action"
          },
          "assignee" : {
            "type" : "string"
          },
          "assigner" : {
            "type" : "string"
          },
          "constraints" : {
            "type" : "array",
            "items" : {
              "$ref" : "#/components/schemas/Constraint"
            }
          },
          "duties" : {
            "type" : "array",
            "items" : {
              "$ref" : "#/components/schemas/Duty"
            }
          },
          "target" : {
            "type" : "string"
          },
          "uid" : {
            "type" : "string"
          }
        }
      },
      "Policy" : {
        "type" : "object",
        "properties" : {
          "@type" : {
            "type" : "string",
            "enum" : [ "SET", "OFFER", "CONTRACT" ]
          },
          "assignee" : {
            "type" : "string"
          },
          "assigner" : {
            "type" : "string"
          },
          "extensibleProperties" : {
            "type" : "object",
            "additionalProperties" : {
              "type" : "object"
            }
          },
          "inheritsFrom" : {
            "type" : "string"
          },
          "obligations" : {
            "type" : "array",
            "items" : {
              "$ref" : "#/components/schemas/Duty"
            }
          },
          "permissions" : {
            "type" : "array",
            "items" : {
              "$ref" : "#/components/schemas/Permission"
            }
          },
          "prohibitions" : {
            "type" : "array",
            "items" : {
              "$ref" : "#/components/schemas/Prohibition"
            }
          },
          "target" : {
            "type" : "string"
          }
        }
      },
      "PolicyDefinition" : {
        "type" : "object",
        "properties" : {
          "createdAt" : {
            "type" : "integer",
            "format" : "int64"
          },
          "id" : {
            "type" : "string"
          },
          "policy" : {
            "$ref" : "#/components/schemas/Policy"
          },
          "uid" : {
            "type" : "string"
          }
        }
      },
      "Prohibition" : {
        "type" : "object",
        "properties" : {
          "action" : {
            "$ref" : "#/components/schemas/Action"
          },
          "assignee" : {
            "type" : "string"
          },
          "assigner" : {
            "type" : "string"
          },
          "constraints" : {
            "type" : "array",
            "items" : {
              "$ref" : "#/components/schemas/Constraint"
            }
          },
          "target" : {
            "type" : "string"
          },
          "uid" : {
            "type" : "string"
          }
        }
      },
      "ProvisionerWebhookRequest" : {
        "required" : [ "apiKeyJwt", "assetId", "contentDataAddress", "resourceDefinitionId", "resourceName" ],
        "type" : "object",
        "properties" : {
          "apiKeyJwt" : {
            "type" : "string"
          },
          "assetId" : {
            "type" : "string"
          },
          "contentDataAddress" : {
            "$ref" : "#/components/schemas/DataAddress"
          },
          "hasToken" : {
            "type" : "boolean"
          },
          "resourceDefinitionId" : {
            "type" : "string"
          },
          "resourceName" : {
            "type" : "string"
          }
        }
      },
      "SelectionRequest" : {
        "type" : "object",
        "properties" : {
          "destination" : {
            "$ref" : "#/components/schemas/DataAddress"
          },
          "source" : {
            "$ref" : "#/components/schemas/DataAddress"
          },
          "strategy" : {
            "type" : "string"
          }
        }
      },
      "TransferId" : {
        "type" : "object",
        "properties" : {
          "id" : {
            "type" : "string"
          }
        }
      },
      "TransferProcessDto" : {
        "type" : "object",
        "properties" : {
          "createdAt" : {
            "type" : "integer",
            "format" : "int64"
          },
          "dataDestination" : {
            "$ref" : "#/components/schemas/DataAddressInformationDto"
          },
          "dataRequest" : {
            "$ref" : "#/components/schemas/DataRequestDto"
          },
          "errorDetail" : {
            "type" : "string"
          },
          "id" : {
            "type" : "string"
          },
          "state" : {
            "type" : "string"
          },
          "stateTimestamp" : {
            "type" : "integer",
            "format" : "int64"
          },
          "type" : {
            "type" : "string"
          },
          "updatedAt" : {
            "type" : "integer",
            "format" : "int64"
          }
        }
      },
      "TransferRequestDto" : {
        "required" : [ "assetId", "connectorAddress", "connectorId", "contractId", "dataDestination", "protocol", "transferType" ],
        "type" : "object",
        "properties" : {
          "assetId" : {
            "type" : "string"
          },
          "connectorAddress" : {
            "type" : "string"
          },
          "connectorId" : {
            "type" : "string"
          },
          "contractId" : {
            "type" : "string"
          },
          "dataDestination" : {
            "$ref" : "#/components/schemas/DataAddress"
          },
          "id" : {
            "type" : "string"
          },
          "managedResources" : {
            "type" : "boolean"
          },
          "properties" : {
            "type" : "object",
            "additionalProperties" : {
              "type" : "string"
            }
          },
          "protocol" : {
            "type" : "string"
          },
          "transferType" : {
            "$ref" : "#/components/schemas/TransferType"
          }
        }
      },
      "TransferState" : {
        "type" : "object",
        "properties" : {
          "state" : {
            "type" : "string"
          }
        }
      },
      "TransferType" : {
        "type" : "object",
        "properties" : {
          "contentType" : {
            "type" : "string"
          },
          "isFinite" : {
            "type" : "boolean"
          }
        }
      }
    }
  }
}