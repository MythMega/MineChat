package com.mine.chat;

import static spark.Spark.*;

import com.google.common.base.Suppliers;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;


public class MineChatClient implements ClientModInitializer {
	private static MinecraftServer server; // Stocke l'instance du serveur

	@Override
	public void onInitializeClient() {
		// Récupère l'instance du serveur au démarrage
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MineChatClient.server = server;
			startHttpServer(); // Démarrer Spark HTTP
		});
	}
	private void startHttpServer() {
		port(8000);
		get("/", (req, res) -> "Serveur HTTP de MineChat actif.");
		get("/sendMessage", (req, res) -> {
			String message = req.queryParams("msg"); // Récupère le message de la requête
			String pseudo = req.queryParams("pseudo"); // Récupère le pseudo de la personne qui envoie la requête
			if (message == null || message.isEmpty()) {
				return "Erreur : Message vide !";
			}
			if(pseudo == null || pseudo.isEmpty()) {
				return "Erreur : Pseudo vide !";
			}

			sendChatMessage(pseudo, message);
			return "Message envoyé : " + message;
		});
		get("/execute", (req, res) -> {
			String cmd = req.queryParams("cmd");
			String pseudo = req.queryParams("pseudo");
			if (cmd == null || cmd.isEmpty()) {
				return "Erreur : Commande vide !";
			}
			if(pseudo == null || pseudo.isEmpty()) {
				return "Erreur : Pseudo vide !";
			}
			executeCommand(cmd, pseudo);
			return "Commande exécuté : " + cmd + " par " + pseudo;
		});
	}
	private void sendChatMessage(String pseudo, String message) {
		if (server != null) {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				player.sendChatMessage(SentMessage.of(SignedMessage.ofUnsigned("[" + pseudo + "] " + message)),
						false,
						new MessageType.Parameters(RegistryEntry.of(new Me)));
				player.sendMessage(Text.literal("[" + pseudo + "] " + message), false);
			}
		}
	}
	private void executeCommand(String command, String pseudo) throws CommandSyntaxException {
		if(server != null) {
			ServerCommandSource source = server.getCommandSource();
			CommandDispatcher<ServerCommandSource> dispatcher = source.getDispatcher();
			ParseResults<ServerCommandSource> parseResults = dispatcher.parse(command, source);
			source.sendFeedback(Suppliers.ofInstance(Text.of("La commande " + command + " est exec par " + pseudo)), true);
			dispatcher.execute(parseResults);
		}
	}
}