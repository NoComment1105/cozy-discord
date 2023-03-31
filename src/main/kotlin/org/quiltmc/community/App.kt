/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(PrivilegedIntent::class)

/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package org.quiltmc.community

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.modules.extra.mappings.extMappings
import com.kotlindiscord.kord.extensions.modules.extra.phishing.DetectionAction
import com.kotlindiscord.kord.extensions.modules.extra.phishing.extPhishing
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.extPluralKit
import com.kotlindiscord.kord.extensions.utils.envOrNull
import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.flow.*
import org.quiltmc.community.cozy.modules.ama.extAma
import org.quiltmc.community.cozy.modules.logs.extLogParser
import org.quiltmc.community.cozy.modules.logs.processors.PiracyProcessor
import org.quiltmc.community.cozy.modules.logs.processors.ProblematicLauncherProcessor
import org.quiltmc.community.cozy.modules.moderation.moderation
import org.quiltmc.community.cozy.modules.rolesync.rolesync
import org.quiltmc.community.cozy.modules.tags.tags
import org.quiltmc.community.cozy.modules.welcome.welcomeChannel
import org.quiltmc.community.database.collections.TagsCollection
import org.quiltmc.community.database.collections.WelcomeChannelCollection
import org.quiltmc.community.logs.NonQuiltLoaderProcessor
import org.quiltmc.community.logs.RuleBreakingModProcessor
import org.quiltmc.community.modes.quilt.extensions.*
import org.quiltmc.community.modes.quilt.extensions.filtering.FilterExtension
import org.quiltmc.community.modes.quilt.extensions.github.GithubExtension
import org.quiltmc.community.modes.quilt.extensions.messagelog.MessageLogExtension
import org.quiltmc.community.modes.quilt.extensions.minecraft.MinecraftExtension
import org.quiltmc.community.modes.quilt.extensions.modhostverify.ModHostingVerificationExtension
import org.quiltmc.community.modes.quilt.extensions.settings.SettingsExtension
import org.quiltmc.community.modes.quilt.extensions.suggestions.SuggestionsExtension
import org.quiltmc.community.modes.quilt.extensions.suggestions.VerificationExtension
import kotlin.time.Duration.Companion.minutes

val MODE = envOrNull("MODE")?.lowercase() ?: "quilt"
val ENVIRONMENT = envOrNull("ENVIRONMENT")?.lowercase() ?: "production"

suspend fun setupCollab() = ExtensibleBot(DISCORD_TOKEN) {
	common()
	database()

	extensions {
		sentry {
			distribution = "collab"
		}
	}
}

suspend fun setupDev() = ExtensibleBot(DISCORD_TOKEN) {
	common()
	database()

	extensions {
		add(::SubteamsExtension)

		extMappings { }

		if (GITHUB_TOKEN != null) {
			add(::GithubExtension)
		}

		sentry {
			distribution = "dev"
		}
	}
}

suspend fun setupQuilt() = ExtensibleBot(DISCORD_TOKEN) {
	common()
	database(true)
	settings()

	chatCommands {
		defaultPrefix = "%"
		enabled = true
	}

	intents {
		+Intents.all
	}

	members {
		all()

		fillPresences = true
	}

	extensions {
		add(::ApplicationsExtension)
		add(::FilterExtension)
		add(::MessageLogExtension)
		add(::MinecraftExtension)
		add(::ModHostingVerificationExtension)
		add(::PKExtension)
		add(::SettingsExtension)
		add(::ShowcaseExtension)
		add(::SuggestionsExtension)
		add(::SyncExtension)
		add(::UtilityExtension)
		add(::VerificationExtension)

		extPluralKit()

		extAma()

		extLogParser {
			// Bundled non-default processors
			processor(PiracyProcessor())
			processor(ProblematicLauncherProcessor())

			// Quilt-specific processors
			processor(NonQuiltLoaderProcessor())
			processor(RuleBreakingModProcessor())

//			@Suppress("TooGenericExceptionCaught")
//			suspend fun predicate(handler: BaseLogHandler, event: Event): Boolean = with(handler) {
//				val predicateLogger = KotlinLogging.logger(
//					"org.quiltmc.community.AppKt.setupQuilt.extLogParser.predicate"
//				)
//
//				val kord: Kord = getKoin().get()
//				val channelId = channelSnowflakeFor(event)
//				val guild = guildFor(event)
//
//				try {
//					val skippableChannelIds = SKIPPABLE_HANDLER_CATEGORIES.mapNotNull {
//						kord.getChannelOf<Category>(it)
//							?.channels
//							?.map { ch -> ch.id }
//							?.toList()
//					}.flatten()
//
//					val isSkippable = identifier in SKIPPABLE_HANDLER_IDS
//
//					if (guild?.id == TOOLCHAIN_GUILD && isSkippable) {
//						predicateLogger.info {
//							"Skipping handler '$identifier' in <#$channelId>: Skippable handler, and on Toolchain"
//						}
//
//						return false
//					}
//
//					if (channelId in skippableChannelIds && isSkippable) {
//						predicateLogger.info {
//							"Skipping handler '$identifier' in <#$channelId>: Skippable handler, and in a dev category"
//						}
//
//						return false
//					}
//
//					predicateLogger.debug { "Passing handler '$identifier' in <#$channelId>" }
//
//					return true
//				} catch (e: Exception) {
//					predicateLogger.warn(e) { "Skipping processor '$identifier' in <#$channelId> due to an error." }
//
//					return true
//				}
//			}
//
//			globalPredicate(::predicate)
		}

		help {
			enableBundledExtension = true
		}

		welcomeChannel(getKoin().get<WelcomeChannelCollection>()) {
			staffCommandCheck {
				hasBaseModeratorRole()
			}

			getLogChannel { _, guild ->
				guild.channels
					.filterIsInstance<GuildMessageChannel>()
					.filter { it.name == "cozy-logs" }
					.lastOrNull()
			}

			refreshDuration = 5.minutes
		}

		tags(getKoin().get<TagsCollection>()) {
			loggingChannelName = "cozy-logs"

			userCommandCheck {
				inQuiltGuild()
			}

			staffCommandCheck {
				hasBaseModeratorRole()
			}
		}

		extPhishing {
			appName = "QuiltMC's Cozy Bot"
			detectionAction = DetectionAction.Kick
			logChannelName = "cozy-logs"
			requiredCommandPermission = null

			check { inQuiltGuild() }
			check { notHasBaseModeratorRole() }
		}

//		userCleanup {
//			maxPendingDuration = 3.days
//			taskDelay = 1.days
//			loggingChannelName = "cozy-logs"
//
//			runAutomatically = true
//
//			guildPredicate {
//				val servers = getKoin().get<ServerSettingsCollection>()
//				val serverEntry = servers.get(it.id)
//
//				serverEntry?.quiltServerType != null
//			}
//
//			commandCheck { hasPermission(Permission.Administrator) }
//		}

		moderation {
			loggingChannelName = "cozy-logs"
			verifiedRoleName = "Verified"

			commandCheck { inQuiltGuild() }
			commandCheck { hasBaseModeratorRole() }
		}

		rolesync {
			roleToSync(
				TOOLCHAIN_DEVELOPER_ROLE,
				COMMUNITY_DEVELOPER_ROLE
			)

			commandCheck { inQuiltGuild() }
			commandCheck { hasBaseModeratorRole() }
		}

		sentry {
			distribution = "community"
		}
	}
}

suspend fun setupShowcase() = ExtensibleBot(DISCORD_TOKEN) {
	common()
	database()
	settings()

	extensions {
		sentry {
			distribution = "showcase"
		}
	}
}

suspend fun main() {
	val bot = when (MODE) {
		"dev" -> setupDev()
		"collab" -> setupCollab()
		"quilt" -> setupQuilt()
		"showcase" -> setupShowcase()

		else -> error("Invalid mode: $MODE")
	}

	bot.start()
}
